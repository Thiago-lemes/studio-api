package com.crative.studio_api.usuario.application.usecase

import com.crative.studio_api.usuario.domain.UsuarioDomain

data class AutenticacaoResponse(
    val token: String,
    val usuario: UsuarioDomain
)