package com.crative.studio_api.academico.matricula.dto

import com.crative.studio_api.academico.matricula.entity.MatriculaEntity
import java.math.BigDecimal

data class MatriculaDetalhada(
    val matricula: MatriculaEntity,
    val nomeAluno: String,
    val modalidadeTurma: String,
    val valorEfetivo: BigDecimal
)
