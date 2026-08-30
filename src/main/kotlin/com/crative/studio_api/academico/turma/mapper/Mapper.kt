package com.crative.studio_api.academico.turma.mapper

import com.crative.studio_api.academico.turma.dto.TurmaDetalhada
import com.crative.studio_api.academico.turma.dto.response.TurmaResponse

fun TurmaDetalhada.toResponse(): TurmaResponse =
    TurmaResponse(
        id = requireNotNull(turma.id),
        modalidade = turma.modalidade,
        professorId = turma.professorId,
        professorNome = professorNome,
        salaId = turma.salaId,
        salaNome = salaNome,
        diasSemana = turma.diasSemana,
        horarioInicio = turma.horarioInicio,
        horarioFim = turma.horarioFim,
        capacidadeMaxima = turma.capacidadeMaxima,
        ativa = turma.ativa
    )