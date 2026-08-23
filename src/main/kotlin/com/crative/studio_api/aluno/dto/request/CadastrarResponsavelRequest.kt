package com.crative.studio_api.aluno.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CadastrarResponsavelRequest(

    @field:NotBlank
    @field:Size(max = 150)
    val nome: String,

    @field:NotBlank
    @field:Size(max = 20)
    val telefone: String,

    @field:Size(max = 14)
    val cpf: String?,

    @field:NotBlank
    @field:Size(max = 50)
    val parentesco: String
)