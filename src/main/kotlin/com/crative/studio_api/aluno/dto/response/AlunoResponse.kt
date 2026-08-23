package com.crative.studio_api.aluno.dto.response

import java.time.LocalDate
import java.util.UUID

data class AlunoResponse(
    val id: UUID,
    val nome: String,
    val telefone: String?,
    val dataNascimento: LocalDate,
    val cpf: String?,
    val idade: Int,
    val menorDeIdade: Boolean,
    val ativo: Boolean
)