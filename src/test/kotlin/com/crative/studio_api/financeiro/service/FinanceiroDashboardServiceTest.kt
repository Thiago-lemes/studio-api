package com.crative.studio_api.financeiro.service

import com.crative.studio_api.academico.matricula.entity.MatriculaEntity
import com.crative.studio_api.academico.matricula.repository.MatriculaRepository
import com.crative.studio_api.financeiro.entity.ContaPagarEntity
import com.crative.studio_api.financeiro.entity.ContaReceberEntity
import com.crative.studio_api.financeiro.entity.PagamentoEntity
import com.crative.studio_api.financeiro.repository.ContaPagarRepository
import com.crative.studio_api.financeiro.repository.ContaReceberRepository
import com.crative.studio_api.financeiro.repository.PagamentoRepository
import com.crative.studio_api.financeiro.types.CategoriaDespesaType
import com.crative.studio_api.financeiro.types.FormaPagamentoType
import com.crative.studio_api.financeiro.types.StatusContaType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.quality.Strictness
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FinanceiroDashboardServiceTest {

    @Mock lateinit var contaReceberRepository: ContaReceberRepository
    @Mock lateinit var contaPagarRepository: ContaPagarRepository
    @Mock lateinit var pagamentoRepository: PagamentoRepository
    @Mock lateinit var matriculaRepository: MatriculaRepository

    private lateinit var service: FinanceiroDashboardService

    private val referencia = YearMonth.of(2026, 4)

    @BeforeEach
    fun setUp() {
        service = FinanceiroDashboardService(
            contaReceberRepository, contaPagarRepository, pagamentoRepository, matriculaRepository
        )
        // por padrão tudo vazio; cada teste preenche só o que exercita
        given(contaReceberRepository.findAllByVencimentoBetween(any(), any())).willReturn(emptyList())
        given(contaReceberRepository.findAllByStatus(any())).willReturn(emptyList())
        given(contaPagarRepository.findAllByVencimentoBetween(any(), any())).willReturn(emptyList())
        given(pagamentoRepository.findAllByPagoEmBetween(any(), any())).willReturn(emptyList())
    }

    private fun receber(
        valor: String,
        status: StatusContaType = StatusContaType.PENDENTE,
        matriculaId: UUID = UUID.randomUUID()
    ) = ContaReceberEntity(
        id = UUID.randomUUID(), matriculaId = matriculaId, referencia = "2026-04",
        valor = BigDecimal(valor), vencimento = LocalDate.of(2026, 4, 10), status = status
    )

    private fun pagar(valor: String) = ContaPagarEntity(
        id = UUID.randomUUID(), descricao = "Aluguel", categoria = CategoriaDespesaType.ALUGUEL,
        valor = BigDecimal(valor), vencimento = LocalDate.of(2026, 4, 5)
    )

    private fun pagamento(valor: String) = PagamentoEntity(
        id = UUID.randomUUID(), contaReceberId = UUID.randomUUID(), valorPago = BigDecimal(valor),
        formaPagamento = FormaPagamentoType.PIX, pagoEm = LocalDateTime.of(2026, 4, 12, 10, 0)
    )

    private fun matricula(alunoId: UUID) = MatriculaEntity(
        id = UUID.randomUUID(), alunoId = alunoId, turmaId = UUID.randomUUID(),
        valorMensalidade = BigDecimal("300.00"), diaVencimento = 10, dataInicio = LocalDate.of(2026, 1, 1)
    )

    @Test
    fun dashboard_vazio_devolve_tudo_zerado() {
        val resultado = service.montar(referencia)

        assertEquals("2026-04", resultado.referencia)
        assertEquals(BigDecimal.ZERO, resultado.totalReceberMes)
        assertEquals(BigDecimal.ZERO, resultado.totalRecebidoMes)
        assertEquals(BigDecimal.ZERO, resultado.totalAtrasado)
        assertEquals(BigDecimal.ZERO, resultado.totalPagarMes)
        assertEquals(0, resultado.quantidadeAlunosInadimplentes)
        assertEquals(BigDecimal.ZERO, resultado.saldoProjetadoMes)
    }

    @Test
    fun total_a_receber_e_o_previsto_do_mes_somando_todos_os_status() {
        given(contaReceberRepository.findAllByVencimentoBetween(any(), any())).willReturn(
            listOf(
                receber("300.00", StatusContaType.PENDENTE),
                receber("200.00", StatusContaType.PAGO),
                receber("100.00", StatusContaType.ATRASADO)
            )
        )

        assertEquals(BigDecimal("600.00"), service.montar(referencia).totalReceberMes)
    }

    @Test
    fun total_recebido_soma_os_pagamentos_do_mes() {
        given(pagamentoRepository.findAllByPagoEmBetween(any(), any()))
            .willReturn(listOf(pagamento("300.00"), pagamento("150.50")))

        assertEquals(BigDecimal("450.50"), service.montar(referencia).totalRecebidoMes)
    }

    @Test
    fun a_janela_do_mes_cobre_do_primeiro_dia_a_meia_noite_ao_ultimo_instante_do_ultimo_dia() {
        service.montar(YearMonth.of(2026, 2))

        verify(contaReceberRepository).findAllByVencimentoBetween(
            eq(LocalDate.of(2026, 2, 1)), eq(LocalDate.of(2026, 2, 28))
        )
        verify(pagamentoRepository).findAllByPagoEmBetween(
            eq(LocalDate.of(2026, 2, 1).atStartOfDay()),
            eq(LocalDate.of(2026, 2, 28).atTime(LocalTime.MAX))
        )
    }

    @Test
    fun fevereiro_de_ano_bissexto_vai_ate_o_dia_vinte_e_nove() {
        service.montar(YearMonth.of(2024, 2))

        verify(contaPagarRepository).findAllByVencimentoBetween(
            eq(LocalDate.of(2024, 2, 1)), eq(LocalDate.of(2024, 2, 29))
        )
    }

    @Test
    fun total_atrasado_ignora_a_competencia_e_pega_todas_as_atrasadas() {
        given(contaReceberRepository.findAllByStatus(StatusContaType.ATRASADO)).willReturn(
            listOf(receber("300.00", StatusContaType.ATRASADO), receber("300.00", StatusContaType.ATRASADO))
        )
        given(matriculaRepository.findById(any())).willReturn(Optional.of(matricula(UUID.randomUUID())))

        assertEquals(BigDecimal("600.00"), service.montar(referencia).totalAtrasado)
    }

    @Test
    fun conta_cada_aluno_inadimplente_uma_vez_mesmo_com_varias_contas_atrasadas() {
        val alunoId = UUID.randomUUID()
        val matriculaId = UUID.randomUUID()
        given(contaReceberRepository.findAllByStatus(StatusContaType.ATRASADO)).willReturn(
            listOf(
                receber("300.00", StatusContaType.ATRASADO, matriculaId),
                receber("300.00", StatusContaType.ATRASADO, matriculaId),
                receber("300.00", StatusContaType.ATRASADO, matriculaId)
            )
        )
        given(matriculaRepository.findById(matriculaId)).willReturn(Optional.of(matricula(alunoId)))

        assertEquals(1, service.montar(referencia).quantidadeAlunosInadimplentes)
    }

    @Test
    fun alunos_distintos_com_contas_atrasadas_sao_contados_separadamente() {
        val matriculaA = UUID.randomUUID()
        val matriculaB = UUID.randomUUID()
        given(contaReceberRepository.findAllByStatus(StatusContaType.ATRASADO)).willReturn(
            listOf(
                receber("300.00", StatusContaType.ATRASADO, matriculaA),
                receber("300.00", StatusContaType.ATRASADO, matriculaB)
            )
        )
        given(matriculaRepository.findById(matriculaA)).willReturn(Optional.of(matricula(UUID.randomUUID())))
        given(matriculaRepository.findById(matriculaB)).willReturn(Optional.of(matricula(UUID.randomUUID())))

        assertEquals(2, service.montar(referencia).quantidadeAlunosInadimplentes)
    }

    @Test
    fun conta_atrasada_com_matricula_orfa_nao_quebra_o_dashboard() {
        val orfa = UUID.randomUUID()
        given(contaReceberRepository.findAllByStatus(StatusContaType.ATRASADO))
            .willReturn(listOf(receber("300.00", StatusContaType.ATRASADO, orfa)))
        given(matriculaRepository.findById(orfa)).willReturn(Optional.empty())

        val resultado = service.montar(referencia)

        assertEquals(BigDecimal("300.00"), resultado.totalAtrasado)
        assertEquals(0, resultado.quantidadeAlunosInadimplentes)
    }

    @Test
    fun saldo_projetado_e_o_previsto_a_receber_menos_o_previsto_a_pagar() {
        given(contaReceberRepository.findAllByVencimentoBetween(any(), any()))
            .willReturn(listOf(receber("1000.00")))
        given(contaPagarRepository.findAllByVencimentoBetween(any(), any()))
            .willReturn(listOf(pagar("400.00"), pagar("100.00")))

        val resultado = service.montar(referencia)

        assertEquals(BigDecimal("1000.00"), resultado.totalReceberMes)
        assertEquals(BigDecimal("500.00"), resultado.totalPagarMes)
        assertEquals(BigDecimal("500.00"), resultado.saldoProjetadoMes)
    }

    @Test
    fun saldo_projetado_pode_ser_negativo() {
        given(contaReceberRepository.findAllByVencimentoBetween(any(), any()))
            .willReturn(listOf(receber("100.00")))
        given(contaPagarRepository.findAllByVencimentoBetween(any(), any()))
            .willReturn(listOf(pagar("900.00")))

        assertEquals(BigDecimal("-800.00"), service.montar(referencia).saldoProjetadoMes)
    }

    @Test
    fun o_recebido_nao_depende_do_previsto_um_pagamento_atrasado_entra_no_mes_do_caixa() {
        // nada vence neste mês, mas um boleto antigo foi pago agora
        given(contaReceberRepository.findAllByVencimentoBetween(any(), any())).willReturn(emptyList())
        given(pagamentoRepository.findAllByPagoEmBetween(any(), any())).willReturn(listOf(pagamento("300.00")))

        val resultado = service.montar(referencia)

        assertEquals(BigDecimal.ZERO, resultado.totalReceberMes)
        assertEquals(BigDecimal("300.00"), resultado.totalRecebidoMes)
    }
}
