package com.crative.studio_api.aluno.dto.response

import java.util.*

data class ResponsavelResponse(
    val id: UUID,
    val alunoId: UUID,
    val nome: String,
    val telefone: String,
    val cpf: String?,
    val parentesco: String
)