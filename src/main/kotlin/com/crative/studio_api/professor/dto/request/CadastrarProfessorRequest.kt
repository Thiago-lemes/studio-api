package com.crative.studio_api.professor.dto.request

import jakarta.validation.constraints.NotBlank

data class CadastrarProfessorRequest(
    @field:NotBlank
    val nome: String,

    val telefone: String,

    val especialidade: String?
)