package com.crative.studio_api.academico.matricula.dto.request

import com.crative.studio_api.academico.matricula.types.StatusMatriculaType
import jakarta.validation.constraints.NotNull

data class AlterarStatusMatriculaRequest(
    @field:NotNull
    val status: StatusMatriculaType
)
