package com.crative.studio_api.financeiro.job

import com.crative.studio_api.academico.matricula.entity.MatriculaEntity
import com.crative.studio_api.academico.matricula.repository.MatriculaRepository
import com.crative.studio_api.academico.matricula.types.StatusMatriculaType
import com.crative.studio_api.financeiro.entity.ContaReceberEntity
import com.crative.studio_api.financeiro.service.ContaReceberService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class GerarMensalidadesJobTest {

    @Mock lateinit var matriculaRepository: MatriculaRepository
    @Mock lateinit var contaReceberService: ContaReceberService

    private lateinit var job: GerarMensalidadesJob

    private val referencia = YearMonth.of(2026, 4)

    @BeforeEach
    fun setUp() {
        job = GerarMensalidadesJob(matriculaRepository, contaReceberService)
    }

    private fun matricula(id: UUID = UUID.randomUUID()) = MatriculaEntity(
        id = id, alunoId = UUID.randomUUID(), turmaId = UUID.randomUUID(),
        valorMensalidade = BigDecimal("300.00"), diaVencimento = 10, dataInicio = LocalDate.of(2026, 1, 1)
    )

    private fun conta(matriculaId: UUID) = ContaReceberEntity(
        id = UUID.randomUUID(), matriculaId = matriculaId, referencia = "2026-04",
        valor = BigDecimal("300.00"), vencimento = LocalDate.of(2026, 4, 10)
    )

    @Test
    fun deve_gerar_uma_cobranca_por_matricula_ativa() {
        val a = matricula()
        val b = matricula()
        given(matriculaRepository.findAllByStatus(StatusMatriculaType.ATIVA)).willReturn(listOf(a, b))
        given(contaReceberService.gerarCobrancaMensal(any(), any())).willAnswer {
            conta((it.arguments[0] as MatriculaEntity).id!!)
        }

        assertEquals(2, job.gerarPara(referencia))

        verify(contaReceberService).gerarCobrancaMensal(eq(a), eq(referencia))
        verify(contaReceberService).gerarCobrancaMensal(eq(b), eq(referencia))
    }

    @Test
    fun deve_considerar_apenas_as_matriculas_ativas() {
        given(matriculaRepository.findAllByStatus(StatusMatriculaType.ATIVA)).willReturn(emptyList())

        assertEquals(0, job.gerarPara(referencia))

        verify(matriculaRepository).findAllByStatus(StatusMatriculaType.ATIVA)
        verify(matriculaRepository, never()).findAllByStatus(StatusMatriculaType.TRANCADA)
        verify(matriculaRepository, never()).findAllByStatus(StatusMatriculaType.CANCELADA)
        verify(contaReceberService, never()).gerarCobrancaMensal(any(), any())
    }

    /**
     * O ponto do job: uma matrícula problemática não pode abortar a geração das demais —
     * senão um dado ruim deixaria o studio sem faturar o mês.
     */
    @Test
    fun uma_matricula_com_falha_nao_impede_as_outras() {
        val ruim = matricula()
        val boaA = matricula()
        val boaB = matricula()
        given(matriculaRepository.findAllByStatus(StatusMatriculaType.ATIVA))
            .willReturn(listOf(boaA, ruim, boaB))
        given(contaReceberService.gerarCobrancaMensal(eq(ruim), any()))
            .willThrow(IllegalStateException("matrícula inconsistente"))
        given(contaReceberService.gerarCobrancaMensal(eq(boaA), any())).willReturn(conta(boaA.id!!))
        given(contaReceberService.gerarCobrancaMensal(eq(boaB), any())).willReturn(conta(boaB.id!!))

        assertEquals(2, job.gerarPara(referencia))

        verify(contaReceberService, times(3)).gerarCobrancaMensal(any(), any())
    }

    @Test
    fun devolve_zero_quando_todas_as_matriculas_falham() {
        val a = matricula()
        given(matriculaRepository.findAllByStatus(StatusMatriculaType.ATIVA)).willReturn(listOf(a))
        given(contaReceberService.gerarCobrancaMensal(any(), any()))
            .willThrow(IllegalStateException("erro"))

        assertEquals(0, job.gerarPara(referencia))
    }

    @Test
    fun o_gatilho_agendado_gera_para_o_mes_corrente() {
        given(matriculaRepository.findAllByStatus(StatusMatriculaType.ATIVA)).willReturn(emptyList())

        job.executar()

        verify(matriculaRepository).findAllByStatus(StatusMatriculaType.ATIVA)
    }

    @Test
    fun a_competencia_informada_e_repassada_ao_servico() {
        val a = matricula()
        val competencia = YearMonth.of(2027, 12)
        given(matriculaRepository.findAllByStatus(StatusMatriculaType.ATIVA)).willReturn(listOf(a))
        given(contaReceberService.gerarCobrancaMensal(any(), any())).willReturn(conta(a.id!!))

        job.gerarPara(competencia)

        verify(contaReceberService).gerarCobrancaMensal(eq(a), eq(competencia))
    }
}
