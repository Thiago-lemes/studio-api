package com.crative.studio_api.usuario.infrastructure.persistence

import com.crative.studio_api.usuario.domain.UsuarioDomain

fun UsuarioDomain.toEntity(): UsuarioEntity = UsuarioEntity(
    id = id,
    nome = nome,
    email = email,
    senhaHash = senhaHash,
    role = role,          // ou o enum equivalente que a entity espera
    professorId = professorId,
    ativo = ativo,
    criadoEm = createdAt
)

fun UsuarioEntity.toDomain(): UsuarioDomain = UsuarioDomain(
    id = id,
    nome = nome,
    email = email,
    senhaHash = senhaHash,
    role = role,
    professorId = professorId,
    ativo = ativo,
    createdAt = criadoEm
)