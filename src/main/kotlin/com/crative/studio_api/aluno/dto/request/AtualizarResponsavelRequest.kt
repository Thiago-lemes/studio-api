package com.crative.studio_api.aluno.dto.request

import jakarta.validation.constraints.NotBlank

data class AtualizarResponsavelRequest(
    @field:NotBlank
    val nome: String,

    @field:NotBlank
    val telefone: String,

    val cpf: String?,

    @field:NotBlank
    val parentesco: String
)