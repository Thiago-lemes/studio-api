package com.crative.studio_api.usuario.infrastructure.web.dto.response

data class LoginResponse(
    val token: String,
    val usuario: UsuarioLogadoResponse
)