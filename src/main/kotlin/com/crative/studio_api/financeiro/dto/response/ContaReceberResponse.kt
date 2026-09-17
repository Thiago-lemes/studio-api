package com.crative.studio_api.financeiro.dto.response

import com.crative.studio_api.financeiro.types.StatusContaType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class ContaReceberResponse(
    val id: UUID,
    val matriculaId: UUID,
    val alunoId: UUID,
    val nomeAluno: String,
    val referencia: String,
    val valor: BigDecimal,
    val vencimento: LocalDate,
    val status: StatusContaType,
    val diasEmAtraso: Int?
)
