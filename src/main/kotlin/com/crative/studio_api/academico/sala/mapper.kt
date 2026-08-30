package com.crative.studio_api.academico.sala

import com.crative.studio_api.academico.sala.dto.response.SalaResponse
import com.crative.studio_api.academico.sala.entity.SalaEntity

fun SalaEntity.toResponse(): SalaResponse =
    SalaResponse(
        id = requireNotNull(id),
        nome = nome,
        capacidade = capacidade
    )