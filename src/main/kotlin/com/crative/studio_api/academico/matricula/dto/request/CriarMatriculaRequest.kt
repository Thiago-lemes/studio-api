package com.crative.studio_api.academico.matricula.dto.request

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class CriarMatriculaRequest(
    @field:NotNull
    val alunoId: UUID,

    @field:NotNull
    val turmaId: UUID,

    @field:NotNull
    @field:DecimalMin(value = "0.01")
    val valorMensalidade: BigDecimal,

    @field:Min(1)
    @field:Max(28)
    val diaVencimento: Int,

    @field:DecimalMin(value = "0.00")
    @field:DecimalMax(value = "100.00")
    val descontoPercentual: BigDecimal = BigDecimal.ZERO,

    @field:NotNull
    val dataInicio: LocalDate
)
