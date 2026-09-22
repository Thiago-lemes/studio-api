package com.crative.studio_api.financeiro.service

import com.crative.studio_api.financeiro.entity.ContaPagarEntity
import com.crative.studio_api.financeiro.entity.ContaReceberEntity
import com.crative.studio_api.financeiro.entity.PagamentoEntity
import com.crative.studio_api.financeiro.exception.PeriodoInvalidoException
import com.crative.studio_api.financeiro.repository.ContaPagarRepository
import com.crative.studio_api.financeiro.repository.ContaReceberRepository
import com.crative.studio_api.financeiro.repository.PagamentoRepository
import com.crative.studio_api.financeiro.types.CategoriaDespesaType
import com.crative.studio_api.financeiro.types.FormaPagamentoType
import com.crative.studio_api.financeiro.types.StatusContaType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.quality.Strictness
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RelatorioFinanceiroServiceTest {

    @Mock lateinit var contaReceberRepository: ContaReceberRepository
    @Mock lateinit var contaPagarRepository: ContaPagarRepository
    @Mock lateinit var pagamentoRepository: PagamentoRepository

    private lateinit var service: RelatorioFinanceiroService

    private val de = LocalDate.of(2026, 9, 1)
    private val ate = LocalDate.of(2026, 9, 30)

    @BeforeEach
    fun setUp() {
        service = RelatorioFinanceiroService(contaReceberRepository, contaPagarRepository, pagamentoRepository)

        given(pagamentoRepository.findAllByPagoEmBetween(any(), any())).willReturn(emptyList())
        given(contaReceberRepository.findAllByVencimentoBetween(any(), any())).willReturn(emptyList())
        given(contaPagarRepository.findAllByVencimentoBetween(any(), any())).willReturn(emptyList())
        given(contaReceberRepository.findAllByStatus(StatusContaType.ATRASADO)).willReturn(emptyList())
    }

    private fun pagamento(
        valor: String,
        forma: FormaPagamentoType = FormaPagamentoType.PIX
    ) = PagamentoEntity(
        contaReceberId = UUID.randomUUID(), valorPago = BigDecimal(valor),
        formaPagamento = forma, pagoEm = LocalDateTime.of(2026, 9, 10, 12, 0)
    )

    private fun aReceber(valor: String, vencimento: LocalDate = LocalDate.of(2026, 9, 5)) =
        ContaReceberEntity(
            matriculaId = UUID.randomUUID(), referencia = "2026-09",
            valor = BigDecimal(valor), vencimento = vencimento
        )

    private fun aPagar(
        valor: String,
        categoria: CategoriaDespesaType = CategoriaDespesaType.ALUGUEL,
        status: StatusContaType = StatusContaType.PENDENTE
    ) = ContaPagarEntity(
        id = UUID.randomUUID(), descricao = "Despesa", categoria = categoria,
        valor = BigDecimal(valor), vencimento = LocalDate.of(2026, 9, 5), status = status
    )

    @Test
    fun deve_recusar_periodo_com_data_final_anterior_a_inicial() {
        assertThrows(PeriodoInvalidoException::class.java) { service.montar(ate, de) }
    }

    @Test
    fun deve_aceitar_periodo_de_um_unico_dia() {
        val resultado = service.montar(de, de)

        assertEquals(de, resultado.de)
        assertEquals(de, resultado.ate)
    }

    /**
     * O par de saldos é o coração do relatório: realizado sai dos pagamentos e das despesas pagas,
     * previsto sai dos vencimentos. Somar os dois regimes seria o erro clássico.
     */
    @Test
    fun deve_separar_saldo_realizado_de_saldo_previsto() {
        given(pagamentoRepository.findAllByPagoEmBetween(any(), any()))
            .willReturn(listOf(pagamento("1000.00"), pagamento("500.00")))
        given(contaReceberRepository.findAllByVencimentoBetween(any(), any()))
            .willReturn(listOf(aReceber("2000.00")))
        given(contaPagarRepository.findAllByVencimentoBetween(any(), any()))
            .willReturn(listOf(aPagar("800.00", status = StatusContaType.PAGO), aPagar("200.00")))

        val resultado = service.montar(de, ate)

        assertEquals(BigDecimal("1500.00"), resultado.totalRecebido)
        assertEquals(BigDecimal("2000.00"), resultado.totalAReceber)
        // só a despesa PAGO entra no realizado; as duas entram no previsto
        assertEquals(BigDecimal("800.00"), resultado.totalPago)
        assertEquals(BigDecimal("1000.00"), resultado.totalAPagar)
        assertEquals(BigDecimal("700.00"), resultado.saldoRealizado)
        assertEquals(BigDecimal("1000.00"), resultado.saldoPrevisto)
    }

    /** Inadimplência é estoque acumulado: a dívida de março continua aberta num relatório de setembro. */
    @Test
    fun deve_incluir_atraso_de_competencia_anterior_ao_periodo() {
        given(contaReceberRepository.findAllByStatus(StatusContaType.ATRASADO))
            .willReturn(listOf(aReceber("300.00", vencimento = LocalDate.of(2026, 3, 10))))

        assertEquals(BigDecimal("300.00"), service.montar(de, ate).totalAtrasado)
    }

    /** Mas não antecipa o futuro: o que vence depois de `ate` ainda não é atraso deste relatório. */
    @Test
    fun nao_deve_incluir_atraso_com_vencimento_posterior_ao_periodo() {
        given(contaReceberRepository.findAllByStatus(StatusContaType.ATRASADO))
            .willReturn(listOf(aReceber("300.00", vencimento = LocalDate.of(2026, 12, 10))))

        assertEquals(BigDecimal.ZERO, service.montar(de, ate).totalAtrasado)
    }

    @Test
    fun deve_agrupar_despesas_por_categoria_da_maior_para_a_menor() {
        given(contaPagarRepository.findAllByVencimentoBetween(any(), any())).willReturn(
            listOf(
                aPagar("100.00", CategoriaDespesaType.MATERIAL),
                aPagar("2000.00", CategoriaDespesaType.ALUGUEL),
                aPagar("50.00", CategoriaDespesaType.MATERIAL)
            )
        )

        val porCategoria = service.montar(de, ate).despesasPorCategoria

        assertEquals(CategoriaDespesaType.ALUGUEL, porCategoria.first().categoria)
        assertEquals(BigDecimal("150.00"), porCategoria.last().total)
        assertEquals(2, porCategoria.last().quantidade)
    }

    @Test
    fun deve_agrupar_recebimentos_por_forma_de_pagamento() {
        given(pagamentoRepository.findAllByPagoEmBetween(any(), any())).willReturn(
            listOf(
                pagamento("100.00", FormaPagamentoType.PIX),
                pagamento("400.00", FormaPagamentoType.PIX),
                pagamento("50.00", FormaPagamentoType.DINHEIRO)
            )
        )

        val resultado = service.montar(de, ate)

        assertEquals(3, resultado.quantidadeRecebimentos)
        assertEquals(FormaPagamentoType.PIX, resultado.recebimentosPorForma.first().formaPagamento)
        assertEquals(BigDecimal("500.00"), resultado.recebimentosPorForma.first().total)
    }

    @Test
    fun periodo_sem_movimento_devolve_zeros_e_nao_nulos() {
        val resultado = service.montar(de, ate)

        assertEquals(BigDecimal.ZERO, resultado.totalRecebido)
        assertEquals(BigDecimal.ZERO, resultado.saldoRealizado)
        assertEquals(emptyList<Any>(), resultado.despesasPorCategoria)
    }
}
