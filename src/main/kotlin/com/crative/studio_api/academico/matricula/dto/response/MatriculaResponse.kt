package com.crative.studio_api.academico.matricula.dto.response

import com.crative.studio_api.academico.matricula.types.StatusMatriculaType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class MatriculaResponse(
    val id: UUID,
    val alunoId: UUID,
    val nomeAluno: String,
    val turmaId: UUID,
    val modalidadeTurma: String,
    val status: StatusMatriculaType,
    val valorMensalidade: BigDecimal,
    val descontoPercentual: BigDecimal,
    val valorEfetivo: BigDecimal,
    val diaVencimento: Int,
    val dataInicio: LocalDate,
    val dataFim: LocalDate?
)
