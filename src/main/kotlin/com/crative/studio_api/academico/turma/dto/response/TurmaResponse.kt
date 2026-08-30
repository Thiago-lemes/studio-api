package com.crative.studio_api.academico.turma.dto.response

import com.crative.studio_api.academico.turma.types.DiaSemanaType
import java.time.LocalTime
import java.util.UUID

data class TurmaResponse(
    val id: UUID,
    val modalidade: String,
    val professorId: UUID,
    val professorNome: String,
    val salaId: UUID,
    val salaNome: String,
    val diasSemana: Set<DiaSemanaType>,
    val horarioInicio: LocalTime,
    val horarioFim: LocalTime,
    val capacidadeMaxima: Int,
    val ativa: Boolean
)