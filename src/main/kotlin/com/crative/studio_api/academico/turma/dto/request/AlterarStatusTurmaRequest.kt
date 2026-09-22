package com.crative.studio_api.academico.turma.dto.request

import jakarta.validation.constraints.NotNull

data class AlterarStatusTurmaRequest(
    @field:NotNull
    val ativa: Boolean
)
