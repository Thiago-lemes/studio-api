package com.crative.studio_api.academico.matricula.service

import com.crative.studio_api.academico.exception.TurmaNaoEncontradaException
import com.crative.studio_api.academico.matricula.dto.MatriculaDetalhada
import com.crative.studio_api.academico.matricula.valorEfetivo
import com.crative.studio_api.academico.matricula.entity.MatriculaEntity
import com.crative.studio_api.academico.matricula.exception.AlunoInativoException
import com.crative.studio_api.academico.matricula.exception.AlunoJaMatriculadoNaTurmaException
import com.crative.studio_api.academico.matricula.exception.DadosFinanceirosInvalidosException
import com.crative.studio_api.academico.matricula.exception.MatriculaNaoEncontradaException
import com.crative.studio_api.academico.matricula.exception.ResponsavelObrigatorioException
import com.crative.studio_api.academico.matricula.exception.TransicaoDeStatusInvalidaException
import com.crative.studio_api.academico.matricula.exception.TurmaInativaException
import com.crative.studio_api.academico.matricula.exception.TurmaSemVagaException
import com.crative.studio_api.academico.matricula.repository.MatriculaRepository
import com.crative.studio_api.academico.matricula.types.StatusMatriculaType
import com.crative.studio_api.academico.turma.entity.TurmaEntity
import com.crative.studio_api.academico.turma.repository.TurmaRepository
import com.crative.studio_api.aluno.entity.AlunoEntity
import com.crative.studio_api.aluno.exception.AlunoNaoEncontradoException
import com.crative.studio_api.aluno.repository.AlunoRepository
import com.crative.studio_api.aluno.repository.ResponsavelRepository
import com.crative.studio_api.financeiro.service.ContaReceberService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Period
import java.time.YearMonth
import java.util.UUID

@Service
class MatriculaService(
    private val repository: MatriculaRepository,
    private val turmaRepository: TurmaRepository,
    private val alunoRepository: AlunoRepository,
    private val responsavelRepository: ResponsavelRepository,
    private val contaReceberService: ContaReceberService
) {

    @Transactional
    fun criar(
        alunoId: UUID,
        turmaId: UUID,
        valorMensalidade: BigDecimal,
        diaVencimento: Int,
        descontoPercentual: BigDecimal,
        dataInicio: LocalDate
    ): MatriculaDetalhada {
        validarDadosFinanceiros(valorMensalidade, diaVencimento, descontoPercentual)

        val aluno = buscarAlunoOuFalhar(alunoId)
        if (!aluno.ativo) {
            throw AlunoInativoException("Aluno inativo não pode ser matriculado")
        }
        validarResponsavelDeMenorDeIdade(aluno)

        val turma = buscarTurmaOuFalhar(turmaId)
        if (!turma.ativa) {
            throw TurmaInativaException("Turma inativa não aceita novas matrículas")
        }

        // A tabela tem UNIQUE(aluno_id, turma_id): uma rematrícula reaproveita a linha
        // cancelada em vez de inserir uma nova (que violaria a constraint).
        val matriculaExistente = repository.findByAlunoIdAndTurmaId(alunoId, turmaId)
        if (matriculaExistente != null && matriculaExistente.status != StatusMatriculaType.CANCELADA) {
            throw AlunoJaMatriculadoNaTurmaException(
                "Aluno já possui matrícula ${matriculaExistente.status} nessa turma"
            )
        }

        validarVagaDisponivel(turma)

        if (matriculaExistente != null) {
            matriculaExistente.status = StatusMatriculaType.ATIVA
            matriculaExistente.valorMensalidade = valorMensalidade
            matriculaExistente.diaVencimento = diaVencimento
            matriculaExistente.descontoPercentual = descontoPercentual
            matriculaExistente.dataInicio = dataInicio
            matriculaExistente.dataFim = null

            val rematricula = repository.save(matriculaExistente)
            gerarPrimeiraCobranca(rematricula)
            return detalhar(rematricula, aluno, turma)
        }

        val matricula = MatriculaEntity(
            alunoId = alunoId,
            turmaId = turmaId,
            valorMensalidade = valorMensalidade,
            diaVencimento = diaVencimento,
            descontoPercentual = descontoPercentual,
            dataInicio = dataInicio
        )

        val matriculaSalva = repository.save(matricula)
        gerarPrimeiraCobranca(matriculaSalva)
        return detalhar(matriculaSalva, aluno, turma)
    }

    fun buscarPorId(id: UUID): MatriculaDetalhada {
        return detalhar(buscarEntidadeOuFalhar(id))
    }

    fun listarPorAluno(alunoId: UUID): List<MatriculaDetalhada> {
        buscarAlunoOuFalhar(alunoId)
        return repository.findAllByAlunoId(alunoId).map(::detalhar)
    }

    fun listarPorTurma(turmaId: UUID): List<MatriculaDetalhada> {
        buscarTurmaOuFalhar(turmaId)
        return repository.findAllByTurmaId(turmaId).map(::detalhar)
    }

    fun listarPorStatus(status: StatusMatriculaType): List<MatriculaDetalhada> {
        return repository.findAllByStatus(status).map(::detalhar)
    }

    /**
     * Transições válidas: ATIVA ↔ TRANCADA, e ambas → CANCELADA.
     * CANCELADA é terminal (rematrícula passa por [criar], que reaproveita a linha).
     */
    @Transactional
    fun alterarStatus(id: UUID, novoStatus: StatusMatriculaType): MatriculaDetalhada {
        val matricula = buscarEntidadeOuFalhar(id)
        val statusAtual = matricula.status

        if (statusAtual == novoStatus) {
            throw TransicaoDeStatusInvalidaException("A matrícula já está com status $novoStatus")
        }
        if (statusAtual == StatusMatriculaType.CANCELADA) {
            throw TransicaoDeStatusInvalidaException(
                "Matrícula cancelada não pode mudar de status — crie uma nova matrícula"
            )
        }

        val turma = buscarTurmaOuFalhar(matricula.turmaId)

        when (novoStatus) {
            StatusMatriculaType.ATIVA -> {
                if (!turma.ativa) {
                    throw TurmaInativaException("Turma inativa não aceita reativação de matrícula")
                }
                validarVagaDisponivel(turma)
                matricula.dataFim = null
            }

            StatusMatriculaType.CANCELADA -> matricula.dataFim = LocalDate.now()

            StatusMatriculaType.TRANCADA -> Unit
        }

        matricula.status = novoStatus
        return detalhar(repository.save(matricula), turma = turma)
    }

    /**
     * Cobrança da competência de início da matrícula, no mesmo commit da matrícula.
     * As competências seguintes ficam por conta do `GerarMensalidadesJob`, que é idempotente
     * — se a matrícula nascer no dia 1, o job não duplica a cobrança gerada aqui.
     */
    private fun gerarPrimeiraCobranca(matricula: MatriculaEntity) {
        contaReceberService.gerarCobrancaMensal(matricula, YearMonth.from(matricula.dataInicio))
    }

    private fun buscarEntidadeOuFalhar(id: UUID): MatriculaEntity {
        return repository.findById(id)
            .orElseThrow { MatriculaNaoEncontradaException("Matrícula não encontrada") }
    }

    private fun buscarAlunoOuFalhar(id: UUID): AlunoEntity {
        return alunoRepository.findById(id)
            .orElseThrow { AlunoNaoEncontradoException("Aluno não encontrado") }
    }

    private fun buscarTurmaOuFalhar(id: UUID): TurmaEntity {
        return turmaRepository.findById(id)
            .orElseThrow { TurmaNaoEncontradaException("Turma não encontrada") }
    }

    private fun detalhar(
        matricula: MatriculaEntity,
        aluno: AlunoEntity? = null,
        turma: TurmaEntity? = null
    ): MatriculaDetalhada {
        val alunoDaMatricula = aluno ?: buscarAlunoOuFalhar(matricula.alunoId)
        val turmaDaMatricula = turma ?: buscarTurmaOuFalhar(matricula.turmaId)

        return MatriculaDetalhada(
            matricula = matricula,
            nomeAluno = alunoDaMatricula.nome,
            modalidadeTurma = turmaDaMatricula.modalidade,
            valorEfetivo = matricula.valorEfetivo()
        )
    }

    private fun validarVagaDisponivel(turma: TurmaEntity) {
        val matriculasAtivas = repository.countByTurmaIdAndStatus(
            requireNotNull(turma.id), StatusMatriculaType.ATIVA
        )
        if (matriculasAtivas >= turma.capacidadeMaxima) {
            throw TurmaSemVagaException("Turma sem vagas disponíveis")
        }
    }

    private fun validarResponsavelDeMenorDeIdade(aluno: AlunoEntity) {
        val idade = Period.between(aluno.dataNascimento, LocalDate.now()).years
        if (idade >= 18) return

        val temResponsavel = responsavelRepository
            .findAllByAlunoId(requireNotNull(aluno.id))
            .isNotEmpty()

        if (!temResponsavel) {
            throw ResponsavelObrigatorioException(
                "Aluno menor de idade precisa de ao menos um responsável cadastrado para ser matriculado"
            )
        }
    }

    private fun validarDadosFinanceiros(
        valorMensalidade: BigDecimal,
        diaVencimento: Int,
        descontoPercentual: BigDecimal
    ) {
        if (valorMensalidade <= BigDecimal.ZERO) {
            throw DadosFinanceirosInvalidosException("Valor da mensalidade deve ser maior que zero")
        }
        if (diaVencimento !in 1..28) {
            throw DadosFinanceirosInvalidosException("Dia de vencimento deve estar entre 1 e 28")
        }
        if (descontoPercentual < BigDecimal.ZERO || descontoPercentual > BigDecimal(100)) {
            throw DadosFinanceirosInvalidosException("Desconto percentual deve estar entre 0 e 100")
        }
    }
}
