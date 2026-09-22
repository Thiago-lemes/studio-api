package com.crative.studio_api.usuario.dto.response

import com.crative.studio_api.usuario.entity.RoleType
import java.util.*

data class UsuarioResponse(
    val id: UUID,
    val nome: String,
    val email: String,
    val role: RoleType,
    val ativo: Boolean
)
