package com.crative.studio_api.academico.turma.service

import com.crative.studio_api.academico.sala.repository.SalaRepository
import com.crative.studio_api.academico.turma.dto.TurmaDetalhada
import com.crative.studio_api.academico.turma.entity.TurmaEntity
import com.crative.studio_api.academico.turma.repository.TurmaRepository
import com.crative.studio_api.academico.turma.types.DiaSemanaType
import com.crative.studio_api.professor.exception.ProfessorNaoEncontradoException
import com.crative.studio_api.professor.repository.ProfessorRepository
import com.crative.studio_api.academico.exception.ChoqueDeHorarioException
import com.crative.studio_api.academico.exception.SalaNaoEncontradaException
import com.crative.studio_api.academico.exception.TurmaNaoEncontradaException
import org.springframework.stereotype.Service
import java.time.LocalTime
import java.util.UUID

@Service
class TurmaService(
    private val repository: TurmaRepository,
    private val professorRepository: ProfessorRepository,
    private val salaRepository: SalaRepository
) {

    fun criar(
        modalidade: String,
        professorId: UUID,
        salaId: UUID,
        diasSemana: Set<DiaSemanaType>,
        horarioInicio: LocalTime,
        horarioFim: LocalTime,
        capacidadeMaxima: Int
    ): TurmaDetalhada {
        validarChoqueDeHorario(salaId, diasSemana, horarioInicio, horarioFim)
        validarChoqueDeHorarioDoProfessor(professorId, diasSemana, horarioInicio, horarioFim)

        val turma = TurmaEntity(
            modalidade = modalidade,
            professorId = professorId,
            salaId = salaId,
            diasSemana = diasSemana.toMutableSet(),
            horarioInicio = horarioInicio,
            horarioFim = horarioFim,
            capacidadeMaxima = capacidadeMaxima
        )

        return detalhar(repository.save(turma))
    }

    fun buscarPorId(id: UUID): TurmaDetalhada {
        return detalhar(buscarEntidadeOuFalhar(id))
    }

    fun listarAtivas(): List<TurmaDetalhada> {
        return repository.findAllByAtivaTrue().map(::detalhar)
    }

    fun atualizar(
        id: UUID,
        professorId: UUID,
        salaId: UUID,
        diasSemana: Set<DiaSemanaType>,
        horarioInicio: LocalTime,
        horarioFim: LocalTime,
        capacidadeMaxima: Int
    ): TurmaDetalhada {
        val turma = buscarEntidadeOuFalhar(id)

        validarChoqueDeHorario(
            salaId = salaId,
            diasSemana = diasSemana,
            horarioInicio = horarioInicio,
            horarioFim = horarioFim,
            ignorarTurmaId = id
        )
        validarChoqueDeHorarioDoProfessor(professorId, diasSemana, horarioInicio, horarioFim, ignorarTurmaId = id)

        turma.professorId = professorId
        turma.salaId = salaId
        turma.diasSemana = diasSemana.toMutableSet()
        turma.horarioInicio = horarioInicio
        turma.horarioFim = horarioFim
        turma.capacidadeMaxima = capacidadeMaxima

        return detalhar(repository.save(turma))
    }

    private fun buscarEntidadeOuFalhar(id: UUID): TurmaEntity {
        return repository.findById(id)
            .orElseThrow { TurmaNaoEncontradaException("Turma não encontrada") }
    }

    private fun detalhar(turma: TurmaEntity): TurmaDetalhada {
        val professor = professorRepository.findById(turma.professorId)
            .orElseThrow { ProfessorNaoEncontradoException("Professor vinculado não encontrado") }

        val sala = salaRepository.findById(turma.salaId)
            .orElseThrow { SalaNaoEncontradaException("Sala vinculada não encontrada") }

        return TurmaDetalhada(turma = turma, professorNome = professor.nome, salaNome = sala.nome)
    }

    private fun validarChoqueDeHorario(
        salaId: UUID,
        diasSemana: Set<DiaSemanaType>,
        horarioInicio: LocalTime,
        horarioFim: LocalTime,
        ignorarTurmaId: UUID? = null
    ) {
        val turmasDaSala = repository.findAllBySalaIdAndAtivaTrue(salaId)
            .filter { it.id != ignorarTurmaId }

        if (haConflito(turmasDaSala, diasSemana, horarioInicio, horarioFim)) {
            throw ChoqueDeHorarioException("Já existe uma turma nessa sala, nesse dia e horário")
        }
    }

    private fun validarChoqueDeHorarioDoProfessor(
        professorId: UUID,
        diasSemana: Set<DiaSemanaType>,
        horarioInicio: LocalTime,
        horarioFim: LocalTime,
        ignorarTurmaId: UUID? = null
    ) {
        val turmasDoProfessor = repository.findAllByProfessorIdAndAtivaTrue(professorId)
            .filter { it.id != ignorarTurmaId }

        if (haConflito(turmasDoProfessor, diasSemana, horarioInicio, horarioFim)) {
            throw ChoqueDeHorarioException("Esse professor já está escalado em outra turma nesse dia e horário")
        }
    }

    private fun haConflito(
        turmasExistentes: List<TurmaEntity>,
        diasSemana: Set<DiaSemanaType>,
        horarioInicio: LocalTime,
        horarioFim: LocalTime
    ): Boolean {
        return turmasExistentes.any { turmaExistente ->
            val diasEmComum = turmaExistente.diasSemana.intersect(diasSemana).isNotEmpty()
            val horarioSobrepoe = horarioInicio < turmaExistente.horarioFim &&
                    turmaExistente.horarioInicio < horarioFim

            diasEmComum && horarioSobrepoe
        }
    }

}