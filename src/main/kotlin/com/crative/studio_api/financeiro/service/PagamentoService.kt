package com.crative.studio_api.financeiro.service

import com.crative.studio_api.financeiro.entity.PagamentoEntity
import com.crative.studio_api.financeiro.exception.ContaJaQuitadaException
import com.crative.studio_api.financeiro.exception.PagamentoNaoEncontradoException
import com.crative.studio_api.financeiro.exception.ValorInvalidoException
import com.crative.studio_api.financeiro.repository.PagamentoRepository
import com.crative.studio_api.financeiro.types.FormaPagamentoType
import com.crative.studio_api.financeiro.types.StatusContaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class PagamentoService(
    private val repository: PagamentoRepository,
    private val contaReceberService: ContaReceberService
) {

    /**
     * Registra o pagamento e quita a `ContaReceber` no mesmo commit.
     * Não há pagamento parcial: qualquer pagamento registrado leva a conta pra PAGO.
     */
    @Transactional
    fun registrar(
        contaReceberId: UUID,
        valorPago: BigDecimal,
        formaPagamento: FormaPagamentoType
    ): PagamentoEntity {
        if (valorPago <= BigDecimal.ZERO) {
            throw ValorInvalidoException("Valor pago deve ser maior que zero")
        }

        val conta = contaReceberService.buscarEntidade(contaReceberId)
        if (conta.status == StatusContaType.PAGO) {
            throw ContaJaQuitadaException("Essa conta a receber já está paga")
        }

        val pagamento = repository.save(
            PagamentoEntity(
                contaReceberId = contaReceberId,
                valorPago = valorPago,
                formaPagamento = formaPagamento
            )
        )

        contaReceberService.marcarComoPago(conta)
        return pagamento
    }

    fun buscarPorId(id: UUID): PagamentoEntity {
        return repository.findById(id)
            .orElseThrow { PagamentoNaoEncontradoException("Pagamento não encontrado") }
    }

    fun listarPorContaReceber(contaReceberId: UUID): List<PagamentoEntity> {
        return repository.findAllByContaReceberId(contaReceberId)
    }
}
