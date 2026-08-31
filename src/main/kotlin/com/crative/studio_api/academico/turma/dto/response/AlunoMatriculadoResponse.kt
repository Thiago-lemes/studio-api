package com.crative.studio_api.academico.turma.dto.response

import com.crative.studio_api.academico.matricula.types.StatusMatriculaType
import java.util.UUID

data class AlunoMatriculadoResponse(
    val alunoId: UUID,
    val nomeAluno: String,
    val matriculaId: UUID,
    val statusMatricula: StatusMatriculaType
)
