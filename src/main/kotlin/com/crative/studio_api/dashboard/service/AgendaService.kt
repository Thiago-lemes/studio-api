package com.crative.studio_api.dashboard.service

import com.crative.studio_api.academico.exception.SalaNaoEncontradaException
import com.crative.studio_api.academico.matricula.repository.MatriculaRepository
import com.crative.studio_api.academico.matricula.types.StatusMatriculaType
import com.crative.studio_api.academico.sala.repository.SalaRepository
import com.crative.studio_api.academico.turma.entity.TurmaEntity
import com.crative.studio_api.academico.turma.repository.TurmaRepository
import com.crative.studio_api.academico.turma.types.DiaSemanaType
import com.crative.studio_api.dashboard.dto.AgendaAulaResponse
import com.crative.studio_api.financeiro.exception.PeriodoInvalidoException
import com.crative.studio_api.professor.exception.ProfessorNaoEncontradoException
import com.crative.studio_api.professor.repository.ProfessorRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Expande a recorrência semanal das turmas ativas em ocorrências datadas — a agenda que o front
 * montava no cliente a partir de `GET /turmas`.
 *
 * Ver [AgendaAulaResponse] para o que essa derivação implica (sem cancelamento, feriado ou reposição).
 */
@Service
class AgendaService(
    private val turmaRepository: TurmaRepository,
    private val professorRepository: ProfessorRepository,
    private val salaRepository: SalaRepository,
    private val matriculaRepository: MatriculaRepository
) {

    companion object {
        /** Teto do intervalo: sem ele, `?de=2020-01-01&ate=2099-12-31` geraria milhões de linhas. */
        private const val MAXIMO_DE_DIAS = 366L

        private val DIA_SEMANA_POR_DAY_OF_WEEK = mapOf(
            DayOfWeek.MONDAY to DiaSemanaType.SEG,
            DayOfWeek.TUESDAY to DiaSemanaType.TER,
            DayOfWeek.WEDNESDAY to DiaSemanaType.QUA,
            DayOfWeek.THURSDAY to DiaSemanaType.QUI,
            DayOfWeek.FRIDAY to DiaSemanaType.SEX,
            DayOfWeek.SATURDAY to DiaSemanaType.SAB,
            DayOfWeek.SUNDAY to DiaSemanaType.DOM
        )
    }

    @Transactional(readOnly = true)
    fun listar(
        de: LocalDate,
        ate: LocalDate,
        professorId: UUID?,
        salaId: UUID?
    ): List<AgendaAulaResponse> {
        validarPeriodo(de, ate)

        val turmas = turmaRepository.findAllByAtivaTrue()
            .filter { professorId == null || it.professorId == professorId }
            .filter { salaId == null || it.salaId == salaId }

        if (turmas.isEmpty()) {
            return emptyList()
        }

        val nomesDeProfessor = professorRepository.findAllById(turmas.map { it.professorId })
            .associate { it.id to it.nome }
        val nomesDeSala = salaRepository.findAllById(turmas.map { it.salaId })
            .associate { it.id to it.nome }

        val matriculasAtivasPorTurma = matriculaRepository
            .findAllByTurmaIdIn(turmas.mapNotNull { it.id })
            .filter { it.status == StatusMatriculaType.ATIVA }
            .groupingBy { it.turmaId }
            .eachCount()

        return datasNoIntervalo(de, ate).flatMap { data ->
            val diaSemana = DIA_SEMANA_POR_DAY_OF_WEEK.getValue(data.dayOfWeek)

            turmas
                .filter { diaSemana in it.diasSemana }
                .map { turma ->
                    montarOcorrencia(
                        turma = turma,
                        data = data,
                        diaSemana = diaSemana,
                        professorNome = nomesDeProfessor[turma.professorId]
                            ?: throw ProfessorNaoEncontradoException("Professor vinculado não encontrado"),
                        salaNome = nomesDeSala[turma.salaId]
                            ?: throw SalaNaoEncontradaException("Sala vinculada não encontrada"),
                        alunosMatriculados = matriculasAtivasPorTurma[turma.id] ?: 0
                    )
                }
        }.sortedWith(compareBy({ it.data }, { it.horarioInicio }, { it.modalidade }))
    }

    private fun montarOcorrencia(
        turma: TurmaEntity,
        data: LocalDate,
        diaSemana: DiaSemanaType,
        professorNome: String,
        salaNome: String,
        alunosMatriculados: Int
    ) = AgendaAulaResponse(
        data = data,
        diaSemana = diaSemana,
        horarioInicio = turma.horarioInicio,
        horarioFim = turma.horarioFim,
        turmaId = requireNotNull(turma.id),
        modalidade = turma.modalidade,
        professorId = turma.professorId,
        professorNome = professorNome,
        salaId = turma.salaId,
        salaNome = salaNome,
        capacidadeMaxima = turma.capacidadeMaxima,
        alunosMatriculados = alunosMatriculados,
        vagasDisponiveis = (turma.capacidadeMaxima - alunosMatriculados).coerceAtLeast(0)
    )

    private fun datasNoIntervalo(de: LocalDate, ate: LocalDate): List<LocalDate> =
        generateSequence(de) { it.plusDays(1) }
            .takeWhile { !it.isAfter(ate) }
            .toList()

    private fun validarPeriodo(de: LocalDate, ate: LocalDate) {
        if (ate.isBefore(de)) {
            throw PeriodoInvalidoException("A data final do período não pode ser anterior à inicial")
        }
        if (ChronoUnit.DAYS.between(de, ate) + 1 > MAXIMO_DE_DIAS) {
            throw PeriodoInvalidoException("O período da agenda não pode passar de $MAXIMO_DE_DIAS dias")
        }
    }
}
