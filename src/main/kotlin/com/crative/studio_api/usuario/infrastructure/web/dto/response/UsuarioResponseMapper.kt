package com.crative.studio_api.usuario.infrastructure.web.dto.response

import com.crative.studio_api.usuario.domain.UsuarioDomain

fun UsuarioDomain.toLogadoResponse(): UsuarioLogadoResponse = UsuarioLogadoResponse(
    id = id,
    nome = nome,
    email = email,
    role = role,
    professorId = professorId
)

fun UsuarioDomain.toCriarUsuarioResponse(): CriarUsuarioResponse = CriarUsuarioResponse(
    id = id,
    nome = nome,
    email = email,
    role = role
)