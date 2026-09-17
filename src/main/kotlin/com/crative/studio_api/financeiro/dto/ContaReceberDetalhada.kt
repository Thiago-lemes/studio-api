package com.crative.studio_api.financeiro.dto

import com.crative.studio_api.financeiro.entity.ContaReceberEntity
import java.util.UUID

data class ContaReceberDetalhada(
    val conta: ContaReceberEntity,
    val alunoId: UUID,
    val nomeAluno: String,
    val diasEmAtraso: Int?
)
