package com.crative.studio_api.financeiro.dto.response

import com.crative.studio_api.financeiro.types.FormaPagamentoType
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class PagamentoResponse(
    val id: UUID,
    val contaReceberId: UUID,
    val valorPago: BigDecimal,
    val formaPagamento: FormaPagamentoType,
    val pagoEm: LocalDateTime
)
