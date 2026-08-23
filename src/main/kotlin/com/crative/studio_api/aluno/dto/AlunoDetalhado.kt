package com.crative.studio_api.aluno.dto

import com.crative.studio_api.aluno.entity.AlunoEntity

data class AlunoDetalhado(
    val aluno: AlunoEntity,
    val idade: Int,
    val menorDeIdade: Boolean
)