package com.crative.studio_api.academico.turma.dto.request

import com.crative.studio_api.academico.turma.types.DiaSemanaType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.LocalTime
import java.util.UUID

data class CriarTurmaRequest(
    @field:NotBlank
    val modalidade: String,

    @field:NotNull
    val professorId: UUID,

    @field:NotNull
    val salaId: UUID,

    @field:NotEmpty
    val diasSemana: Set<DiaSemanaType>,

    @field:NotNull
    val horarioInicio: LocalTime,

    @field:NotNull
    val horarioFim: LocalTime,

    @field:Positive
    val capacidadeMaxima: Int
)