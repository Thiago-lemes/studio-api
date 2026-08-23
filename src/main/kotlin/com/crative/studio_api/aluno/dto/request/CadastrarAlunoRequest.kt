package com.crative.studio_api.aluno.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PastOrPresent
import java.time.LocalDate

data class CadastrarAlunoRequest(
    @field:NotBlank
    val nome: String,

    val telefone: String?,

    @field:PastOrPresent
    val dataNascimento: LocalDate,

    @field:NotBlank
    val cpf: String
)