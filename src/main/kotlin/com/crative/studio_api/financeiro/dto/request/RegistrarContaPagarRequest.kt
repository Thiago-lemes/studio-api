package com.crative.studio_api.financeiro.dto.request

import com.crative.studio_api.financeiro.types.CategoriaDespesaType
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate

data class RegistrarContaPagarRequest(
    @field:NotBlank
    val descricao: String,

    @field:NotNull
    val categoria: CategoriaDespesaType,

    @field:NotNull
    @field:DecimalMin(value = "0.01")
    val valor: BigDecimal,

    @field:NotNull
    val vencimento: LocalDate
)
