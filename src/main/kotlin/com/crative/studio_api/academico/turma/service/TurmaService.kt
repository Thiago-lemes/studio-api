package com.crative.studio_api.academico.turma.service

import com.crative.studio_api.academico.exception.ChoqueDeHorarioException
import com.crative.studio_api.academico.exception.SalaNaoEncontradaException
import com.crative.studio_api.academico.exception.TurmaComMatriculasAtivasException
import com.crative.studio_api.academico.exception.TurmaNaoEncontradaException
import com.crative.studio_api.academico.matricula.repository.MatriculaRepository
import com.crative.studio_api.academico.matricula.types.StatusMatriculaType
import com.crative.studio_api.academico.sala.repository.SalaRepository
import com.crative.studio_api.academico.turma.dto.TurmaDetalhada
import com.crative.studio_api.academico.turma.dto.response.AlunoMatriculadoResponse
import com.crative.studio_api.academico.turma.entity.TurmaEntity
import com.crative.studio_api.academico.turma.repository.TurmaRepository
import com.crative.studio_api.academico.turma.types.DiaSemanaType
import com.crative.studio_api.aluno.exception.AlunoNaoEncontradoException
import com.crative.studio_api.aluno.repository.AlunoRepository
import com.crative.studio_api.professor.exception.ProfessorNaoEncontradoException
import com.crative.studio_api.professor.repository.ProfessorRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime
import java.util.*

@Service
class TurmaService(
    private val repository: TurmaRepository,
    private val professorRepository: ProfessorRepository,
    private val salaRepository: SalaRepository,
    private val matriculaRepository: MatriculaRepository,
    private val alunoRepository: AlunoRepository
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
        validarVinculosExistem(professorId, salaId)
        validarChoqueDeHorarioNaSala(salaId, diasSemana, horarioInicio, horarioFim)
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

    fun buscarEntidade(id: UUID): TurmaEntity {
        return buscarEntidadeOuFalhar(id)
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

        validarVinculosExistem(professorId, salaId)
        validarChoqueDeHorarioNaSala(salaId, diasSemana, horarioInicio, horarioFim, ignorarTurmaId = id)
        validarChoqueDeHorarioDoProfessor(professorId, diasSemana, horarioInicio, horarioFim, ignorarTurmaId = id)

        turma.professorId = professorId
        turma.salaId = salaId
        turma.diasSemana = diasSemana.toMutableSet()
        turma.horarioInicio = horarioInicio
        turma.horarioFim = horarioFim
        turma.capacidadeMaxima = capacidadeMaxima

        return detalhar(repository.save(turma))
    }

    /**
     * Soft delete, como aluno e professor: a coluna `ativa` já existia e `listarAtivas` já a
     * respeitava — o que faltava era um endpoint capaz de mudá-la, e por isso nenhuma turma criada
     * podia ser encerrada.
     *
     * Encerrar é recusado enquanto houver matrícula ATIVA: as mensalidades continuariam sendo
     * geradas pelo job para uma turma que não existe mais na grade. Trancar ou cancelar as
     * matrículas vem antes.
     *
     * Reativar revalida choque de horário — a sala e o professor podem ter sido ocupados por outra
     * turma enquanto esta estava fora da grade.
     */
    @Transactional
    fun alterarStatus(id: UUID, ativa: Boolean): TurmaDetalhada {
        val turma = buscarEntidadeOuFalhar(id)

        if (turma.ativa == ativa) {
            return detalhar(turma)
        }

        if (ativa) {
            validarChoqueDeHorarioNaSala(
                turma.salaId, turma.diasSemana, turma.horarioInicio, turma.horarioFim, ignorarTurmaId = id
            )
            validarChoqueDeHorarioDoProfessor(
                turma.professorId, turma.diasSemana, turma.horarioInicio, turma.horarioFim, ignorarTurmaId = id
            )
        } else {
            val matriculasAtivas = matriculaRepository.countByTurmaIdAndStatus(id, StatusMatriculaType.ATIVA)
            if (matriculasAtivas > 0) {
                throw TurmaComMatriculasAtivasException(
                    "Esta turma ainda tem $matriculasAtivas matrícula(s) ativa(s). " +
                            "Cancele ou tranque as matrículas antes de encerrá-la."
                )
            }
        }

        turma.ativa = ativa
        return detalhar(repository.save(turma))
    }

    fun listarAlunosMatriculados(turmaId: UUID): List<AlunoMatriculadoResponse> {
        buscarEntidadeOuFalhar(turmaId)

        val matriculasAtivas = matriculaRepository.findAllByTurmaIdAndStatus(turmaId, StatusMatriculaType.ATIVA)

        return matriculasAtivas.map { matricula ->
            val aluno = alunoRepository.findById(matricula.alunoId)
                .orElseThrow { AlunoNaoEncontradoException("Aluno vinculado não encontrado") }

            AlunoMatriculadoResponse(
                alunoId = matricula.alunoId,
                nomeAluno = aluno.nome,
                matriculaId = requireNotNull(matricula.id),
                statusMatricula = matricula.status
            )
        }
    }

    private fun buscarEntidadeOuFalhar(id: UUID): TurmaEntity {
        return repository.findById(id)
            .orElseThrow { TurmaNaoEncontradaException("Turma não encontrada") }
    }

    /**
     * Professor e sala são checados aqui, antes de qualquer `save`. Sem isso o insert só
     * falharia na foreign key, devolvendo um erro de infraestrutura no lugar do 404 que a
     * API promete — e o `detalhar()` nunca chegaria a rodar.
     */
    private fun validarVinculosExistem(professorId: UUID, salaId: UUID) {
        if (!professorRepository.existsById(professorId)) {
            throw ProfessorNaoEncontradoException("Professor não encontrado")
        }
        if (!salaRepository.existsById(salaId)) {
            throw SalaNaoEncontradaException("Sala não encontrada")
        }
    }

    private fun detalhar(turma: TurmaEntity): TurmaDetalhada {
        val professor = professorRepository.findById(turma.professorId)
            .orElseThrow { ProfessorNaoEncontradoException("Professor vinculado não encontrado") }

        val sala = salaRepository.findById(turma.salaId)
            .orElseThrow { SalaNaoEncontradaException("Sala vinculada não encontrada") }

        val matriculasAtivas = matriculaRepository.countByTurmaIdAndStatus(
            requireNotNull(turma.id), StatusMatriculaType.ATIVA
        )
        val vagasDisponiveis = turma.capacidadeMaxima - matriculasAtivas

        return TurmaDetalhada(
            turma = turma,
            professorNome = professor.nome,
            salaNome = sala.nome,
            vagasDisponiveis = vagasDisponiveis
        )
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

    private fun validarChoqueDeHorarioNaSala(
        salaId: UUID,
        diasSemana: Set<DiaSemanaType>,
        horarioInicio: LocalTime,
        horarioFim: LocalTime,
        ignorarTurmaId: UUID? = null
    ) {
        val turmasDaSala = repository.findAllBySalaIdAndAtivaTrue(salaId).filter { it.id != ignorarTurmaId }
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
        val turmasDoProfessor = repository.findAllByProfessorIdAndAtivaTrue(professorId).filter { it.id != ignorarTurmaId }
        if (haConflito(turmasDoProfessor, diasSemana, horarioInicio, horarioFim)) {
            throw ChoqueDeHorarioException("Esse professor já está escalado em outra turma nesse dia e horário")
        }
    }
}