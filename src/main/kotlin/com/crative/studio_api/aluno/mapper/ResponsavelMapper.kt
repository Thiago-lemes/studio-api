package com.crative.studio_api.aluno.mapper

import com.crative.studio_api.aluno.dto.response.ResponsavelResponse
import com.crative.studio_api.aluno.entity.ResponsavelEntity

fun ResponsavelEntity.toResponse(): ResponsavelResponse =
    ResponsavelResponse(
        id = requireNotNull(id),
        alunoId = alunoId,
        nome = nome,
        telefone = telefone,
        cpf = cpf,
        parentesco = parentesco
    )