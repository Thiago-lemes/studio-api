package com.crative.studio_api.usuario.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class TrocarSenhaRequest(
    @field:NotBlank
    val senhaAtual: String,

    // Mesmo mínimo do CompletarCadastroRequest: se a senha escolhida no convite tem 8 caracteres,
    // não faz sentido a troca posterior aceitar menos.
    @field:NotBlank
    @field:Size(min = 8, message = "Nova senha deve ter no mínimo 8 caracteres")
    val novaSenha: String
)
