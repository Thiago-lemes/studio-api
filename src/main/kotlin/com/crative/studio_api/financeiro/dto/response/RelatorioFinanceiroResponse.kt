package com.crative.studio_api.financeiro.dto.response

import com.crative.studio_api.financeiro.types.CategoriaDespesaType
import com.crative.studio_api.financeiro.types.FormaPagamentoType
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Relatório de um intervalo livre de datas — o que o front vinha somando no navegador para
 * habilitar o PDF.
 *
 * Os números vêm em dois regimes, e misturá-los é o erro clássico deste relatório:
 *
 * - **realizado** (`totalRecebido`, `totalPago`): dinheiro que entrou ou saiu *dentro do período*,
 *   pela data do pagamento. Uma mensalidade de agosto paga em setembro conta em setembro.
 * - **previsto** (`totalAReceber`, `totalAPagar`): tudo que *vence* no período, em qualquer status.
 *
 * Por isso `saldoRealizado` e `saldoPrevisto` andam separados: um responde "quanto sobrou no
 * caixa", o outro "quanto deveria sobrar". Somar os dois não significa nada.
 *
 * `totalAtrasado` foge da janela de propósito: é o estoque de inadimplência acumulado até `ate`,
 * de qualquer competência, porque a dívida velha não deixa de existir por estar fora do período.
 */
data class RelatorioFinanceiroResponse(
    val de: LocalDate,
    val ate: LocalDate,

    val totalRecebido: BigDecimal,
    val totalAReceber: BigDecimal,
    val totalAtrasado: BigDecimal,

    val totalPago: BigDecimal,
    val totalAPagar: BigDecimal,

    val saldoRealizado: BigDecimal,
    val saldoPrevisto: BigDecimal,

    val quantidadeRecebimentos: Int,
    val quantidadeDespesas: Int,

    val despesasPorCategoria: List<TotalPorCategoriaResponse>,
    val recebimentosPorForma: List<TotalPorFormaResponse>
)

data class TotalPorCategoriaResponse(
    val categoria: CategoriaDespesaType,
    val total: BigDecimal,
    val quantidade: Int
)

data class TotalPorFormaResponse(
    val formaPagamento: FormaPagamentoType,
    val total: BigDecimal,
    val quantidade: Int
)
