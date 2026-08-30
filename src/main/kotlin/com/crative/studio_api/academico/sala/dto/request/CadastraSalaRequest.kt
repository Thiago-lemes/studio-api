package com.crative.studio_api.academico.sala.dto.request

import jakarta.validation.constraints.NotBlank

data class CadastraSalaRequest(
    @field:NotBlank
    val nome: String,
    val capacidade: Int
)