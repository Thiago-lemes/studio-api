package com.crative.studio_api.financeiro.service

import com.crative.studio_api.financeiro.entity.ContaReceberEntity
import com.crative.studio_api.financeiro.entity.PagamentoEntity
import com.crative.studio_api.financeiro.exception.ContaJaQuitadaException
import com.crative.studio_api.financeiro.exception.ContaReceberNaoEncontradaException
import com.crative.studio_api.financeiro.exception.PagamentoNaoEncontradoException
import com.crative.studio_api.financeiro.exception.ValorInvalidoException
import com.crative.studio_api.financeiro.repository.PagamentoRepository
import com.crative.studio_api.financeiro.types.FormaPagamentoType
import com.crative.studio_api.financeiro.types.StatusContaType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InOrder
import org.mockito.Mock
import org.mockito.Mockito.inOrder
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class PagamentoServiceTest {

    @Mock lateinit var repository: PagamentoRepository
    @Mock lateinit var contaReceberService: ContaReceberService

    private lateinit var service: PagamentoService

    private val contaId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service = PagamentoService(repository, contaReceberService)
    }

    private fun conta(status: StatusContaType = StatusContaType.PENDENTE) = ContaReceberEntity(
        id = contaId,
        matriculaId = UUID.randomUUID(),
        referencia = "2026-04",
        valor = BigDecimal("300.00"),
        vencimento = LocalDate.now().plusDays(3),
        status = status
    )

    @Test
    fun deve_registrar_pagamento_e_quitar_a_conta() {
        val alvo = conta()
        given(contaReceberService.buscarEntidade(contaId)).willReturn(alvo)
        given(repository.save(any<PagamentoEntity>())).willAnswer { it.arguments[0] }

        val resultado = service.registrar(contaId, BigDecimal("300.00"), FormaPagamentoType.PIX)

        assertEquals(contaId, resultado.contaReceberId)
        assertEquals(BigDecimal("300.00"), resultado.valorPago)
        assertEquals(FormaPagamentoType.PIX, resultado.formaPagamento)
        verify(contaReceberService).marcarComoPago(alvo)
    }

    @Test
    fun o_pagamento_e_gravado_antes_da_baixa_da_conta() {
        val alvo = conta()
        given(contaReceberService.buscarEntidade(contaId)).willReturn(alvo)
        given(repository.save(any<PagamentoEntity>())).willAnswer { it.arguments[0] }

        service.registrar(contaId, BigDecimal("300.00"), FormaPagamentoType.PIX)

        val ordem: InOrder = inOrder(repository, contaReceberService)
        ordem.verify(repository).save(any<PagamentoEntity>())
        ordem.verify(contaReceberService).marcarComoPago(alvo)
    }

    @Test
    fun nao_ha_pagamento_parcial_qualquer_valor_quita_a_conta() {
        val alvo = conta()
        given(contaReceberService.buscarEntidade(contaId)).willReturn(alvo)
        given(repository.save(any<PagamentoEntity>())).willAnswer { it.arguments[0] }

        // paga 50 de uma conta de 300 e a conta ainda assim é quitada
        service.registrar(contaId, BigDecimal("50.00"), FormaPagamentoType.DINHEIRO)

        verify(contaReceberService).marcarComoPago(alvo)
    }

    @Test
    fun deve_aceitar_pagamento_de_conta_atrasada() {
        val alvo = conta(status = StatusContaType.ATRASADO)
        given(contaReceberService.buscarEntidade(contaId)).willReturn(alvo)
        given(repository.save(any<PagamentoEntity>())).willAnswer { it.arguments[0] }

        service.registrar(contaId, BigDecimal("300.00"), FormaPagamentoType.BOLETO)

        verify(contaReceberService).marcarComoPago(alvo)
    }

    @Test
    fun deve_recusar_valor_zero_ou_negativo_antes_de_buscar_a_conta() {
        assertThrows(ValorInvalidoException::class.java) {
            service.registrar(contaId, BigDecimal.ZERO, FormaPagamentoType.PIX)
        }
        assertThrows(ValorInvalidoException::class.java) {
            service.registrar(contaId, BigDecimal("-10.00"), FormaPagamentoType.PIX)
        }
        verify(contaReceberService, never()).buscarEntidade(any())
        verify(repository, never()).save(any<PagamentoEntity>())
    }

    @Test
    fun deve_recusar_pagamento_de_conta_ja_paga() {
        given(contaReceberService.buscarEntidade(contaId)).willReturn(conta(status = StatusContaType.PAGO))

        assertThrows(ContaJaQuitadaException::class.java) {
            service.registrar(contaId, BigDecimal("300.00"), FormaPagamentoType.PIX)
        }
        verify(repository, never()).save(any<PagamentoEntity>())
        verify(contaReceberService, never()).marcarComoPago(any())
    }

    @Test
    fun deve_propagar_conta_inexistente() {
        given(contaReceberService.buscarEntidade(contaId))
            .willThrow(ContaReceberNaoEncontradaException("Conta a receber não encontrada"))

        assertThrows(ContaReceberNaoEncontradaException::class.java) {
            service.registrar(contaId, BigDecimal("300.00"), FormaPagamentoType.PIX)
        }
        verify(repository, never()).save(any<PagamentoEntity>())
    }

    @Test
    fun o_pagamento_gravado_carrega_a_forma_e_o_valor_informados() {
        given(contaReceberService.buscarEntidade(contaId)).willReturn(conta())
        given(repository.save(any<PagamentoEntity>())).willAnswer { it.arguments[0] }

        service.registrar(contaId, BigDecimal("123.45"), FormaPagamentoType.CARTAO)

        verify(repository).save(check {
            assertEquals(contaId, it.contaReceberId)
            assertEquals(BigDecimal("123.45"), it.valorPago)
            assertEquals(FormaPagamentoType.CARTAO, it.formaPagamento)
        })
    }

    @Test
    fun deve_buscar_pagamento_por_id() {
        val id = UUID.randomUUID()
        val pagamento = PagamentoEntity(
            id = id, contaReceberId = contaId, valorPago = BigDecimal("300.00"),
            formaPagamento = FormaPagamentoType.PIX
        )
        given(repository.findById(id)).willReturn(Optional.of(pagamento))

        assertEquals(id, service.buscarPorId(id).id)
    }

    @Test
    fun buscar_pagamento_inexistente_falha() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.empty())

        assertThrows(PagamentoNaoEncontradoException::class.java) { service.buscarPorId(id) }
    }

    @Test
    fun deve_listar_pagamentos_da_conta() {
        given(repository.findAllByContaReceberId(contaId)).willReturn(
            listOf(
                PagamentoEntity(contaReceberId = contaId, valorPago = BigDecimal("300.00"), formaPagamento = FormaPagamentoType.PIX)
            )
        )

        assertEquals(1, service.listarPorContaReceber(contaId).size)
    }
}
