package com.crative.studio_api.professor.dto.request

import jakarta.validation.constraints.NotBlank

data class AtualizarProfessorRequest(
    @field:NotBlank
    val nome: String,

    val telefone: String,

    val especialidade: String?
)