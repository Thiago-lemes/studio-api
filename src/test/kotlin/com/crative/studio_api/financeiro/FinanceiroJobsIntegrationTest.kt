package com.crative.studio_api.financeiro

import com.crative.studio_api.academico.matricula.types.StatusMatriculaType
import com.crative.studio_api.financeiro.entity.ContaReceberEntity
import com.crative.studio_api.financeiro.job.GerarMensalidadesJob
import com.crative.studio_api.financeiro.job.MarcarContasAtrasadasJob
import com.crative.studio_api.financeiro.types.StatusContaType
import com.crative.studio_api.shared.AbstractIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/**
 * Os jobs contra o banco real. O ponto central é a idempotência: o
 * `UNIQUE(matricula_id, referencia)` é a garantia de que rodar o job duas vezes no mesmo mês
 * não cobra o aluno duas vezes — e isso só pode ser verificado com Postgres de verdade.
 */
class FinanceiroJobsIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var gerarMensalidadesJob: GerarMensalidadesJob
    @Autowired lateinit var marcarContasAtrasadasJob: MarcarContasAtrasadasJob

    private fun matriculaAtiva(
        nome: String = "Joana",
        cpf: String = "111.111.111-11",
        valor: BigDecimal = BigDecimal("300.00"),
        desconto: BigDecimal = BigDecimal.ZERO,
        diaVencimento: Int = 10,
        turmaId: UUID = criarTurma().id!!
    ) = criarMatricula(
        alunoId = criarAluno(nome = nome, cpf = cpf).id!!,
        turmaId = turmaId,
        valorMensalidade = valor,
        descontoPercentual = desconto,
        diaVencimento = diaVencimento
    )

    // ---------- geração de mensalidades ----------

    @Test
    fun deve_gerar_uma_cobranca_por_matricula_ativa() {
        val turma = criarTurma(capacidadeMaxima = 10)
        matriculaAtiva(nome = "Joana", cpf = "111.111.111-11", turmaId = turma.id!!)
        matriculaAtiva(nome = "Pedro", cpf = "222.222.222-22", turmaId = turma.id!!)

        assertEquals(2, gerarMensalidadesJob.gerarPara(YearMonth.of(2026, 5)))
        assertEquals(2, contaReceberRepository.count())
    }

    @Test
    fun rodar_o_job_duas_vezes_na_mesma_competencia_nao_duplica_cobranca() {
        matriculaAtiva()
        val competencia = YearMonth.of(2026, 5)

        gerarMensalidadesJob.gerarPara(competencia)
        gerarMensalidadesJob.gerarPara(competencia)
        gerarMensalidadesJob.gerarPara(competencia)

        assertEquals(1, contaReceberRepository.count())
    }

    @Test
    fun competencias_diferentes_geram_cobrancas_diferentes() {
        val matricula = matriculaAtiva()

        gerarMensalidadesJob.gerarPara(YearMonth.of(2026, 5))
        gerarMensalidadesJob.gerarPara(YearMonth.of(2026, 6))

        val referencias = contaReceberRepository.findAllByMatriculaId(matricula.id!!).map { it.referencia }
        assertEquals(setOf("2026-05", "2026-06"), referencias.toSet())
    }

    @Test
    fun matriculas_trancadas_e_canceladas_nao_sao_cobradas() {
        val turma = criarTurma(capacidadeMaxima = 10)
        val ativa = matriculaAtiva(nome = "Ativa", cpf = "111.111.111-11", turmaId = turma.id!!)
        val trancada = matriculaAtiva(nome = "Trancada", cpf = "222.222.222-22", turmaId = turma.id!!)
        val cancelada = matriculaAtiva(nome = "Cancelada", cpf = "333.333.333-33", turmaId = turma.id!!)

        trancada.status = StatusMatriculaType.TRANCADA
        cancelada.status = StatusMatriculaType.CANCELADA
        matriculaRepository.saveAll(listOf(trancada, cancelada))

        assertEquals(1, gerarMensalidadesJob.gerarPara(YearMonth.of(2026, 5)))
        assertEquals(
            listOf(ativa.id),
            contaReceberRepository.findAll().map { it.matriculaId }
        )
    }

    @Test
    fun a_cobranca_gerada_usa_o_valor_com_desconto_e_o_dia_de_vencimento_da_matricula() {
        matriculaAtiva(valor = BigDecimal("400.00"), desconto = BigDecimal("25.00"), diaVencimento = 7)

        gerarMensalidadesJob.gerarPara(YearMonth.of(2026, 5))

        val conta = contaReceberRepository.findAll().single()
        assertEquals(BigDecimal("300.00"), conta.valor)
        assertEquals(LocalDate.of(2026, 5, 7), conta.vencimento)
    }

    @Test
    fun cobranca_de_competencia_passada_ja_nasce_atrasada() {
        matriculaAtiva()

        gerarMensalidadesJob.gerarPara(YearMonth.from(LocalDate.now().minusMonths(3)))

        assertEquals(StatusContaType.ATRASADO, contaReceberRepository.findAll().single().status)
    }

    @Test
    fun cobranca_de_competencia_futura_nasce_pendente() {
        matriculaAtiva()

        gerarMensalidadesJob.gerarPara(YearMonth.from(LocalDate.now().plusMonths(3)))

        assertEquals(StatusContaType.PENDENTE, contaReceberRepository.findAll().single().status)
    }

    @Test
    fun sem_matricula_ativa_o_job_nao_gera_nada() {
        assertEquals(0, gerarMensalidadesJob.gerarPara(YearMonth.of(2026, 5)))
        assertEquals(0, contaReceberRepository.count())
    }

    // ---------- marcação de atraso ----------

    @Test
    fun deve_marcar_como_atrasada_a_pendente_vencida() {
        val matricula = matriculaAtiva()
        val vencida = contaReceberRepository.save(
            ContaReceberEntity(
                matriculaId = matricula.id!!, referencia = "2026-01", valor = BigDecimal("300.00"),
                vencimento = LocalDate.now().minusDays(5), status = StatusContaType.PENDENTE
            )
        )

        assertEquals(1, marcarContasAtrasadasJob.executar())
        assertEquals(
            StatusContaType.ATRASADO,
            contaReceberRepository.findById(vencida.id!!).orElseThrow().status
        )
    }

    @Test
    fun nao_marca_a_conta_que_vence_hoje() {
        val matricula = matriculaAtiva()
        contaReceberRepository.save(
            ContaReceberEntity(
                matriculaId = matricula.id!!, referencia = "2026-01", valor = BigDecimal("300.00"),
                vencimento = LocalDate.now(), status = StatusContaType.PENDENTE
            )
        )

        assertEquals(0, marcarContasAtrasadasJob.executar())
        assertEquals(StatusContaType.PENDENTE, contaReceberRepository.findAll().single().status)
    }

    @Test
    fun nao_reabre_conta_paga_mesmo_vencida() {
        val matricula = matriculaAtiva()
        contaReceberRepository.save(
            ContaReceberEntity(
                matriculaId = matricula.id!!, referencia = "2026-01", valor = BigDecimal("300.00"),
                vencimento = LocalDate.now().minusDays(60), status = StatusContaType.PAGO
            )
        )

        assertEquals(0, marcarContasAtrasadasJob.executar())
        assertEquals(StatusContaType.PAGO, contaReceberRepository.findAll().single().status)
    }

    @Test
    fun rodar_o_job_de_atraso_duas_vezes_nao_reprocessa_as_mesmas_contas() {
        val matricula = matriculaAtiva()
        contaReceberRepository.save(
            ContaReceberEntity(
                matriculaId = matricula.id!!, referencia = "2026-01", valor = BigDecimal("300.00"),
                vencimento = LocalDate.now().minusDays(5), status = StatusContaType.PENDENTE
            )
        )

        assertEquals(1, marcarContasAtrasadasJob.executar())
        // na segunda passada a conta já é ATRASADO e sai do filtro
        assertEquals(0, marcarContasAtrasadasJob.executar())
    }

    @Test
    fun marca_varias_contas_de_uma_vez() {
        val turma = criarTurma(capacidadeMaxima = 10)
        val a = matriculaAtiva(nome = "A", cpf = "111.111.111-11", turmaId = turma.id!!)
        val b = matriculaAtiva(nome = "B", cpf = "222.222.222-22", turmaId = turma.id!!)
        listOf(a, b).forEachIndexed { indice, matricula ->
            contaReceberRepository.save(
                ContaReceberEntity(
                    matriculaId = matricula.id!!, referencia = "2026-0${indice + 1}",
                    valor = BigDecimal("300.00"), vencimento = LocalDate.now().minusDays(10L + indice),
                    status = StatusContaType.PENDENTE
                )
            )
        }

        assertEquals(2, marcarContasAtrasadasJob.executar())
        assertTrue(contaReceberRepository.findAll().all { it.status == StatusContaType.ATRASADO })
    }
}
