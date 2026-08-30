package com.crative.studio_api.professor.mapper

import com.crative.studio_api.professor.dto.response.ProfessorResponse
import com.crative.studio_api.professor.entity.ProfessorEntity

fun ProfessorEntity.toResponse(): ProfessorResponse =
    ProfessorResponse(
        id = requireNotNull(id),
        nome = nome,
        telefone = telefone,
        especialidade = especialidade,
        ativo = ativo
    )