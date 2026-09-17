package com.crative.studio_api.financeiro.dto.response

import java.math.BigDecimal

data class DashboardFinanceiroResponse(
    val referencia: String,
    val totalReceberMes: BigDecimal,
    val totalRecebidoMes: BigDecimal,
    val totalAtrasado: BigDecimal,
    val quantidadeAlunosInadimplentes: Int,
    val totalPagarMes: BigDecimal,
    val saldoProjetadoMes: BigDecimal
)
