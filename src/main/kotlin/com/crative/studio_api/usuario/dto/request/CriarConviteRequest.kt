package com.crative.studio_api.usuario.dto.request

import com.crative.studio_api.usuario.entity.RoleType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

/**
 * O ADMIN informa só e-mail e papel. No caso de PROFESSOR, a ficha (nome, telefone,
 * especialidade) nasce quando a própria pessoa completa o cadastro.
 */
data class CriarConviteRequest(
    @field:NotBlank(message = "Email é obrigatório")
    @field:Email(message = "Email inválido")
    val email: String,

    val role: RoleType
)
