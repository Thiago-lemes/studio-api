package com.crative.studio_api.usuario.infrastructure.web.dto.request

import com.crative.studio_api.usuario.domain.RoleType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class CriarUsuarioRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    val nome: String,

    @field:NotBlank(message = "Email é obrigatório")
    @field:Email(message = "Email inválido")
    val email: String,

    @field:NotBlank(message = "Senha é obrigatória")
    val senha: String,

    val role: RoleType
)