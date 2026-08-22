package com.crative.studio_api.usuario.dto.response

import com.crative.studio_api.usuario.entity.UsuarioEntity

data class ResultadoAutenticacao(
    val token: String,
    val usuario: UsuarioEntity
)