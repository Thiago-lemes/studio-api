package com.crative.studio_api.professor.dto.response

import java.util.UUID

data class ProfessorResponse(
    val id: UUID,
    val nome: String,
    val telefone: String?,
    val especialidade: String?,
    val ativo: Boolean
)