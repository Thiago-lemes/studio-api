package com.crative.studio_api.aluno.dto.request

import jakarta.validation.constraints.NotBlank

data class AtualizarAlunoRequest(
    @field:NotBlank
    val nome: String,

    val telefone: String?,
)