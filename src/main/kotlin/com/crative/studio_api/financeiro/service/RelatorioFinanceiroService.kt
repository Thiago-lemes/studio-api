package com.crative.studio_api.financeiro.service

import com.crative.studio_api.financeiro.dto.response.RelatorioFinanceiroResponse
import com.crative.studio_api.financeiro.dto.response.TotalPorCategoriaResponse
import com.crative.studio_api.financeiro.dto.response.TotalPorFormaResponse
import com.crative.studio_api.financeiro.exception.PeriodoInvalidoException
import com.crative.studio_api.financeiro.repository.ContaPagarRepository
import com.crative.studio_api.financeiro.repository.ContaReceberRepository
import com.crative.studio_api.financeiro.repository.PagamentoRepository
import com.crative.studio_api.financeiro.types.StatusContaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

/**
 * Fechamento de um intervalo livre de datas. Difere do [FinanceiroDashboardService], que é preso a
 * uma competência (AAAA-MM) e serve à tela de acompanhamento do mês corrente — aqui o período é
 * arbitrário e a saída é detalhada o bastante para virar PDF.
 *
 * As definições de realizado × previsto estão documentadas no [RelatorioFinanceiroResponse].
 */
@Service
class RelatorioFinanceiroService(
    private val contaReceberRepository: ContaReceberRepository,
    private val contaPagarRepository: ContaPagarRepository,
    private val pagamentoRepository: PagamentoRepository
) {

    @Transactional(readOnly = true)
    fun montar(de: LocalDate, ate: LocalDate): RelatorioFinanceiroResponse {
        if (ate.isBefore(de)) {
            throw PeriodoInvalidoException("A data final do período não pode ser anterior à inicial")
        }
        val pagamentosRecebidos = pagamentoRepository
            .findAllByPagoEmBetween(de.atStartOfDay(), ate.atTime(LocalTime.MAX))

        val contasAReceber = contaReceberRepository.findAllByVencimentoBetween(de, ate)
        val contasAPagar = contaPagarRepository.findAllByVencimentoBetween(de, ate)

        val despesasPagas = contasAPagar.filter { it.status == StatusContaType.PAGO }

        val totalRecebido = pagamentosRecebidos.somar { it.valorPago }
        val totalAReceber = contasAReceber.somar { it.valor }
        val totalPago = despesasPagas.somar { it.valor }
        val totalAPagar = contasAPagar.somar { it.valor }

        // Fora da janela de propósito: inadimplência é estoque acumulado até `ate`, não fluxo do
        // período — a dívida de março continua aberta num relatório de setembro.
        val totalAtrasado = contaReceberRepository
            .findAllByStatus(StatusContaType.ATRASADO)
            .filter { !it.vencimento.isAfter(ate) }
            .somar { it.valor }

        return RelatorioFinanceiroResponse(
            de = de,
            ate = ate,
            totalRecebido = totalRecebido,
            totalAReceber = totalAReceber,
            totalAtrasado = totalAtrasado,
            totalPago = totalPago,
            totalAPagar = totalAPagar,
            saldoRealizado = totalRecebido - totalPago,
            saldoPrevisto = totalAReceber - totalAPagar,
            quantidadeRecebimentos = pagamentosRecebidos.size,
            quantidadeDespesas = contasAPagar.size,
            despesasPorCategoria = contasAPagar
                .groupBy { it.categoria }
                .map { (categoria, contas) ->
                    TotalPorCategoriaResponse(categoria, contas.somar { it.valor }, contas.size)
                }
                .sortedByDescending { it.total },
            recebimentosPorForma = pagamentosRecebidos
                .groupBy { it.formaPagamento }
                .map { (forma, pagamentos) ->
                    TotalPorFormaResponse(forma, pagamentos.somar { it.valorPago }, pagamentos.size)
                }
                .sortedByDescending { it.total }
        )
    }

    private fun <T> List<T>.somar(seletor: (T) -> BigDecimal): BigDecimal =
        fold(BigDecimal.ZERO) { acumulado, item -> acumulado + seletor(item) }
}
