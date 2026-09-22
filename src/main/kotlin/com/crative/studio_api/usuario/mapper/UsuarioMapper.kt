package com.crative.studio_api.usuario.mapper

import com.crative.studio_api.usuario.dto.response.UsuarioLogadoResponse
import com.crative.studio_api.usuario.dto.response.UsuarioResponse
import com.crative.studio_api.usuario.entity.UsuarioEntity


fun UsuarioEntity.toUsuarioResponse(): UsuarioResponse =
    UsuarioResponse(
        id = id,
        nome = nome,
        email = email,
        role = role,
        ativo = ativo
    )


fun UsuarioEntity.toLogadoResponse(): UsuarioLogadoResponse =
    UsuarioLogadoResponse(
        id = id,
        nome = nome,
        email = email,
        role = role,
        professorId = professorId
    )

