package com.crative.studio_api.dashboard.dto

import com.crative.studio_api.financeiro.dto.response.DashboardFinanceiroResponse

/**
 * Visão de abertura do sistema, num único GET.
 *
 * Existe para substituir as três listagens completas que o front baixava só para contar linhas —
 * o custo crescia com a base inteira para exibir cinco números.
 */
data class DashboardGeralResponse(
    val academico: ResumoAcademicoResponse,
    val financeiro: DashboardFinanceiroResponse
)

/**
 * `vagasDisponiveis` é a soma das vagas livres de todas as turmas ativas, e **não**
 * `capacidadeTotal - matriculasAtivas`: turma lotada não empresta vaga para turma vazia, então
 * qualquer excedente por turma é descartado no cálculo.
 */
data class ResumoAcademicoResponse(
    val alunosAtivos: Int,
    val professoresAtivos: Int,
    val turmasAtivas: Int,
    val matriculasAtivas: Int,
    val capacidadeTotal: Int,
    val vagasDisponiveis: Int
)
