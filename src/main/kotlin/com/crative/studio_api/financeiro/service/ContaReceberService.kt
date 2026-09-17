package com.crative.studio_api.financeiro.service

import com.crative.studio_api.academico.matricula.entity.MatriculaEntity
import com.crative.studio_api.academico.matricula.exception.MatriculaNaoEncontradaException
import com.crative.studio_api.academico.matricula.repository.MatriculaRepository
import com.crative.studio_api.academico.matricula.valorEfetivo
import com.crative.studio_api.aluno.exception.AlunoNaoEncontradoException
import com.crative.studio_api.aluno.repository.AlunoRepository
import com.crative.studio_api.financeiro.dto.ContaReceberDetalhada
import com.crative.studio_api.financeiro.entity.ContaReceberEntity
import com.crative.studio_api.financeiro.exception.ContaReceberNaoEncontradaException
import com.crative.studio_api.financeiro.repository.ContaReceberRepository
import com.crative.studio_api.financeiro.types.StatusContaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class ContaReceberService(
    private val repository: ContaReceberRepository,
    private val matriculaRepository: MatriculaRepository,
    private val alunoRepository: AlunoRepository
) {

    /**
     * Gera a cobrança da matrícula na competência informada.
     *
     * Idempotente: a tabela tem `UNIQUE(matricula_id, referencia)` e o método devolve a conta
     * já existente em vez de tentar inserir de novo — o job mensal pode rodar duas vezes no
     * mesmo mês sem duplicar cobrança.
     */
    @Transactional
    fun gerarCobrancaMensal(matricula: MatriculaEntity, referencia: YearMonth): ContaReceberEntity {
        val matriculaId = requireNotNull(matricula.id)
        val referenciaFormatada = referencia.toString()

        if (repository.existsByMatriculaIdAndReferencia(matriculaId, referenciaFormatada)) {
            return repository.findAllByMatriculaId(matriculaId)
                .first { it.referencia == referenciaFormatada }
        }

        val vencimento = referencia.atDay(matricula.diaVencimento)

        val conta = ContaReceberEntity(
            matriculaId = matriculaId,
            referencia = referenciaFormatada,
            valor = matricula.valorEfetivo(),
            vencimento = vencimento,
            status = if (vencimento.isBefore(LocalDate.now())) StatusContaType.ATRASADO else StatusContaType.PENDENTE
        )

        return repository.save(conta)
    }

    fun buscarPorId(id: UUID): ContaReceberDetalhada {
        return detalhar(buscarEntidadeOuFalhar(id))
    }

    fun buscarEntidade(id: UUID): ContaReceberEntity {
        return buscarEntidadeOuFalhar(id)
    }

    fun listar(status: StatusContaType?, matriculaId: UUID?): List<ContaReceberDetalhada> {
        val contas = when {
            matriculaId != null && status != null -> repository.findAllByMatriculaIdAndStatus(matriculaId, status)
            matriculaId != null -> repository.findAllByMatriculaId(matriculaId)
            status != null -> repository.findAllByStatus(status)
            else -> repository.findAll()
        }
        return contas.map(::detalhar)
    }

    @Transactional
    fun marcarComoPago(conta: ContaReceberEntity): ContaReceberEntity {
        conta.status = StatusContaType.PAGO
        return repository.save(conta)
    }

    /** Usado pelo job diário: PENDENTE com vencimento anterior a hoje vira ATRASADO. */
    @Transactional
    fun marcarContasAtrasadas(): List<ContaReceberEntity> {
        val vencidas = repository.findAllByStatusAndVencimentoBefore(StatusContaType.PENDENTE, LocalDate.now())
        vencidas.forEach { it.status = StatusContaType.ATRASADO }
        return repository.saveAll(vencidas)
    }

    private fun buscarEntidadeOuFalhar(id: UUID): ContaReceberEntity {
        return repository.findById(id)
            .orElseThrow { ContaReceberNaoEncontradaException("Conta a receber não encontrada") }
    }

    private fun detalhar(conta: ContaReceberEntity): ContaReceberDetalhada {
        val matricula = matriculaRepository.findById(conta.matriculaId)
            .orElseThrow { MatriculaNaoEncontradaException("Matrícula vinculada não encontrada") }

        val aluno = alunoRepository.findById(matricula.alunoId)
            .orElseThrow { AlunoNaoEncontradoException("Aluno vinculado não encontrado") }

        return ContaReceberDetalhada(
            conta = conta,
            alunoId = matricula.alunoId,
            nomeAluno = aluno.nome,
            diasEmAtraso = calcularDiasEmAtraso(conta)
        )
    }

    private fun calcularDiasEmAtraso(conta: ContaReceberEntity): Int? {
        if (conta.status != StatusContaType.ATRASADO) return null
        return ChronoUnit.DAYS.between(conta.vencimento, LocalDate.now()).toInt()
    }
}
