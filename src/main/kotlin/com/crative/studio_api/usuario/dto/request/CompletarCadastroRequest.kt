package com.crative.studio_api.usuario.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CompletarCadastroRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    val nome: String,

    @field:NotBlank(message = "Senha é obrigatória")
    @field:Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    val senha: String,

    /** Obrigatório quando o convite é de PROFESSOR — é o que identifica a ficha. Ignorado nos demais papéis. */
    val telefone: String? = null,

    /** Opcional, só usado quando o convite é de PROFESSOR. */
    val especialidade: String? = null
)
