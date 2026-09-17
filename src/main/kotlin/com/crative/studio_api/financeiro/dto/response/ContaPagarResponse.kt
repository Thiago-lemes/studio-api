package com.crative.studio_api.financeiro.dto.response

import com.crative.studio_api.financeiro.types.CategoriaDespesaType
import com.crative.studio_api.financeiro.types.StatusContaType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class ContaPagarResponse(
    val id: UUID,
    val descricao: String,
    val categoria: CategoriaDespesaType,
    val valor: BigDecimal,
    val vencimento: LocalDate,
    val status: StatusContaType,
    val pagoEm: LocalDateTime?
)
