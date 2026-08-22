package com.crative.studio_api.usuario.dto.response

data class LoginResponse(
    val token: String,
    val usuario: UsuarioLogadoResponse
)