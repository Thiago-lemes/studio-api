package com.crative.studio_api.financeiro.service

import com.crative.studio_api.financeiro.entity.ContaPagarEntity
import com.crative.studio_api.financeiro.exception.ContaJaQuitadaException
import com.crative.studio_api.financeiro.exception.ContaPagarNaoEncontradaException
import com.crative.studio_api.financeiro.exception.DescricaoObrigatoriaException
import com.crative.studio_api.financeiro.exception.ValorInvalidoException
import com.crative.studio_api.financeiro.repository.ContaPagarRepository
import com.crative.studio_api.financeiro.types.CategoriaDespesaType
import com.crative.studio_api.financeiro.types.StatusContaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Service
class ContaPagarService(
    private val repository: ContaPagarRepository
) {

    fun registrar(
        descricao: String,
        categoria: CategoriaDespesaType,
        valor: BigDecimal,
        vencimento: LocalDate
    ): ContaPagarEntity {
        if (descricao.isBlank()) {
            throw DescricaoObrigatoriaException("Descrição da conta a pagar é obrigatória")
        }
        if (valor <= BigDecimal.ZERO) {
            throw ValorInvalidoException("Valor da conta deve ser maior que zero")
        }

        val conta = ContaPagarEntity(
            descricao = descricao,
            categoria = categoria,
            valor = valor,
            vencimento = vencimento,
            status = statusInicial(vencimento)
        )

        return repository.save(conta)
    }

    fun buscarPorId(id: UUID): ContaPagarEntity {
        return repository.findById(id)
            .orElseThrow { ContaPagarNaoEncontradaException("Conta a pagar não encontrada") }
    }

    fun listar(status: StatusContaType?): List<ContaPagarEntity> {
        return if (status == null) repository.findAll() else repository.findAllByStatus(status)
    }

    /**
     * Recalcula o status a partir do novo vencimento, em vez de preservar o antigo: adiar uma
     * despesa que estava ATRASADO para uma data futura tem de devolvê-la a PENDENTE, senão a
     * correção do erro deixa a conta marcada como atrasada para sempre.
     *
     * Conta já quitada não é editável — mexer no valor depois da baixa reescreveria o caixa
     * de um mês já fechado.
     */
    @Transactional
    fun atualizar(
        id: UUID,
        descricao: String,
        categoria: CategoriaDespesaType,
        valor: BigDecimal,
        vencimento: LocalDate
    ): ContaPagarEntity {
        val conta = buscarPorId(id)
        validarEditavel(conta, "editada")

        if (descricao.isBlank()) {
            throw DescricaoObrigatoriaException("Descrição da conta a pagar é obrigatória")
        }
        if (valor <= BigDecimal.ZERO) {
            throw ValorInvalidoException("Valor da conta deve ser maior que zero")
        }

        conta.descricao = descricao
        conta.categoria = categoria
        conta.valor = valor
        conta.vencimento = vencimento
        conta.status = statusInicial(vencimento)

        return repository.save(conta)
    }

    /**
     * Exclusão física, como em sala e diferente de aluno/professor: uma despesa lançada por engano
     * não é histórico que valha preservar — é ruído no relatório. O que já foi pago, porém, é
     * caixa realizado e não sai daqui.
     */
    @Transactional
    fun remover(id: UUID) {
        val conta = buscarPorId(id)
        validarEditavel(conta, "removida")

        repository.delete(conta)
    }

    private fun validarEditavel(conta: ContaPagarEntity, acao: String) {
        if (conta.status == StatusContaType.PAGO) {
            throw ContaJaQuitadaException("Conta a pagar já quitada não pode ser $acao")
        }
    }

    @Transactional
    fun quitar(id: UUID, dataPagamento: LocalDateTime): ContaPagarEntity {
        val conta = buscarPorId(id)

        if (conta.status == StatusContaType.PAGO) {
            throw ContaJaQuitadaException("Conta a pagar já está quitada")
        }

        conta.status = StatusContaType.PAGO
        conta.pagoEm = dataPagamento
        return repository.save(conta)
    }

    /** Conta registrada com vencimento já passado nasce ATRASADO — o job diário só cobre as futuras. */
    private fun statusInicial(vencimento: LocalDate): StatusContaType =
        if (vencimento.isBefore(LocalDate.now())) StatusContaType.ATRASADO else StatusContaType.PENDENTE
}
