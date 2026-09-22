package com.crative.studio_api.dashboard.service

import com.crative.studio_api.academico.matricula.repository.MatriculaRepository
import com.crative.studio_api.academico.matricula.types.StatusMatriculaType
import com.crative.studio_api.academico.turma.repository.TurmaRepository
import com.crative.studio_api.aluno.repository.AlunoRepository
import com.crative.studio_api.dashboard.dto.DashboardGeralResponse
import com.crative.studio_api.dashboard.dto.ResumoAcademicoResponse
import com.crative.studio_api.financeiro.service.FinanceiroDashboardService
import com.crative.studio_api.professor.repository.ProfessorRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class DashboardGeralService(
    private val alunoRepository: AlunoRepository,
    private val professorRepository: ProfessorRepository,
    private val turmaRepository: TurmaRepository,
    private val matriculaRepository: MatriculaRepository,
    private val financeiroDashboardService: FinanceiroDashboardService
) {

    @Transactional(readOnly = true)
    fun montar(referencia: YearMonth): DashboardGeralResponse {
        return DashboardGeralResponse(
            academico = montarResumoAcademico(),
            // Reaproveita o dashboard financeiro em vez de recalcular: as definições de
            // recebido/previsto/atrasado já estão lá, e duplicá-las aqui seria a receita para os
            // dois números divergirem com o tempo.
            financeiro = financeiroDashboardService.montar(referencia)
        )
    }

    private fun montarResumoAcademico(): ResumoAcademicoResponse {
        val turmasAtivas = turmaRepository.findAllByAtivaTrue()

        // Uma consulta para todas as turmas, em vez de uma por turma dentro do laço: são poucas
        // turmas hoje, mas o custo cresceria junto com a grade justamente na tela de abertura.
        val matriculasAtivasPorTurma = turmasAtivas
            .mapNotNull { it.id }
            .takeIf { it.isNotEmpty() }
            ?.let { ids ->
                matriculaRepository.findAllByTurmaIdIn(ids)
                    .filter { it.status == StatusMatriculaType.ATIVA }
                    .groupingBy { it.turmaId }
                    .eachCount()
            }
            ?: emptyMap()

        // coerceAtLeast(0): turma que teve a capacidade reduzida depois de cheia ficaria com vagas
        // negativas, e a soma mentiria para baixo no total do estúdio.
        val vagasDisponiveis = turmasAtivas.sumOf { turma ->
            val ocupadas = matriculasAtivasPorTurma[turma.id] ?: 0
            (turma.capacidadeMaxima - ocupadas).coerceAtLeast(0)
        }

        return ResumoAcademicoResponse(
            alunosAtivos = alunoRepository.countByAtivoTrue(),
            professoresAtivos = professorRepository.countByAtivoTrue(),
            turmasAtivas = turmasAtivas.size,
            matriculasAtivas = matriculaRepository.countByStatus(StatusMatriculaType.ATIVA),
            capacidadeTotal = turmasAtivas.sumOf { it.capacidadeMaxima },
            vagasDisponiveis = vagasDisponiveis
        )
    }
}
