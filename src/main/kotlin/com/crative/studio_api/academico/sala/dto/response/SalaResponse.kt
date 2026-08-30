package com.crative.studio_api.academico.sala.dto.response

import java.util.UUID

data class SalaResponse(
    val id: UUID,
    val nome: String,
    val capacidade: Int
)
