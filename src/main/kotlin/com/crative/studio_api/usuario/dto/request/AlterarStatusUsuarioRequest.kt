package com.crative.studio_api.usuario.dto.request

import jakarta.validation.constraints.NotNull

data class AlterarStatusUsuarioRequest(
    @field:NotNull
    val ativo: Boolean
)
