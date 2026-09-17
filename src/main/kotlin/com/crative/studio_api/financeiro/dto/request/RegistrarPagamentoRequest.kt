package com.crative.studio_api.financeiro.dto.request

import com.crative.studio_api.financeiro.types.FormaPagamentoType
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.util.UUID

data class RegistrarPagamentoRequest(
    @field:NotNull
    val contaReceberId: UUID,

    @field:NotNull
    @field:DecimalMin(value = "0.01")
    val valorPago: BigDecimal,

    @field:NotNull
    val formaPagamento: FormaPagamentoType
)
