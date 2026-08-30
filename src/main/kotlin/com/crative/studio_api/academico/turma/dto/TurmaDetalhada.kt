package com.crative.studio_api.academico.turma.dto

import com.crative.studio_api.academico.turma.entity.TurmaEntity

data class TurmaDetalhada(
    val turma: TurmaEntity,
    val professorNome: String,
    val salaNome: String
)