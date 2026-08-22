package com.crative.studio_api.usuario.infrastructure.web.dto.response

import com.crative.studio_api.usuario.domain.RoleType
import java.util.UUID

data class UsuarioLogadoResponse(
    val id: UUID,
    val nome: String,
    val email: String,
    val role: RoleType,
    val professorId: UUID?
)