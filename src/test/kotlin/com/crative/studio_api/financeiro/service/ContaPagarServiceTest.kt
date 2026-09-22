package com.crative.studio_api.financeiro.service

import com.crative.studio_api.financeiro.entity.ContaPagarEntity
import com.crative.studio_api.financeiro.exception.ContaJaQuitadaException
import com.crative.studio_api.financeiro.exception.ContaPagarNaoEncontradaException
import com.crative.studio_api.financeiro.exception.DescricaoObrigatoriaException
import com.crative.studio_api.financeiro.exception.ValorInvalidoException
import com.crative.studio_api.financeiro.repository.ContaPagarRepository
import com.crative.studio_api.financeiro.types.CategoriaDespesaType
import com.crative.studio_api.financeiro.types.StatusContaType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ContaPagarServiceTest {

    @Mock
    lateinit var repository: ContaPagarRepository

    private lateinit var service: ContaPagarService

    @BeforeEach
    fun setUp() {
        service = ContaPagarService(repository)
    }

    private fun conta(
        id: UUID? = UUID.randomUUID(),
        status: StatusContaType = StatusContaType.PENDENTE,
        pagoEm: LocalDateTime? = null
    ) = ContaPagarEntity(
        id = id,
        descricao = "Aluguel",
        categoria = CategoriaDespesaType.ALUGUEL,
        valor = BigDecimal("2500.00"),
        vencimento = LocalDate.now().plusDays(5),
        status = status,
        pagoEm = pagoEm
    )

    @Test
    fun conta_com_vencimento_futuro_nasce_pendente() {
        given(repository.save(any<ContaPagarEntity>())).willAnswer { it.arguments[0] }

        val resultado = service.registrar(
            "Aluguel", CategoriaDespesaType.ALUGUEL, BigDecimal("2500.00"), LocalDate.now().plusDays(10)
        )

        assertEquals(StatusContaType.PENDENTE, resultado.status)
        assertNull(resultado.pagoEm)
    }

    @Test
    fun conta_com_vencimento_hoje_nasce_pendente() {
        given(repository.save(any<ContaPagarEntity>())).willAnswer { it.arguments[0] }

        val resultado = service.registrar(
            "Aluguel", CategoriaDespesaType.ALUGUEL, BigDecimal("2500.00"), LocalDate.now()
        )

        assertEquals(StatusContaType.PENDENTE, resultado.status)
    }

    @Test
    fun conta_registrada_com_vencimento_passado_nasce_atrasada() {
        given(repository.save(any<ContaPagarEntity>())).willAnswer { it.arguments[0] }

        val resultado = service.registrar(
            "Aluguel de março", CategoriaDespesaType.ALUGUEL, BigDecimal("2500.00"), LocalDate.now().minusDays(1)
        )

        assertEquals(StatusContaType.ATRASADO, resultado.status)
    }

    @Test
    fun deve_recusar_descricao_em_branco() {
        assertThrows(DescricaoObrigatoriaException::class.java) {
            service.registrar("   ", CategoriaDespesaType.OUTROS, BigDecimal("10.00"), LocalDate.now())
        }
        verify(repository, never()).save(any<ContaPagarEntity>())
    }

    @Test
    fun deve_recusar_valor_zero_ou_negativo() {
        assertThrows(ValorInvalidoException::class.java) {
            service.registrar("Aluguel", CategoriaDespesaType.ALUGUEL, BigDecimal.ZERO, LocalDate.now())
        }
        assertThrows(ValorInvalidoException::class.java) {
            service.registrar("Aluguel", CategoriaDespesaType.ALUGUEL, BigDecimal("-1"), LocalDate.now())
        }
    }

    @Test
    fun deve_buscar_conta_por_id() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(conta(id = id)))

        assertEquals(id, service.buscarPorId(id).id)
    }

    @Test
    fun deve_lancar_excecao_ao_buscar_id_inexistente() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.empty())

        assertThrows(ContaPagarNaoEncontradaException::class.java) { service.buscarPorId(id) }
    }

    @Test
    fun listar_sem_filtro_devolve_todas() {
        given(repository.findAll()).willReturn(listOf(conta(), conta()))

        assertEquals(2, service.listar(null).size)
        verify(repository, never()).findAllByStatus(any())
    }

    @Test
    fun listar_com_status_filtra_no_repositorio() {
        given(repository.findAllByStatus(StatusContaType.ATRASADO))
            .willReturn(listOf(conta(status = StatusContaType.ATRASADO)))

        assertEquals(1, service.listar(StatusContaType.ATRASADO).size)
        verify(repository, never()).findAll()
    }

    @Test
    fun deve_quitar_conta_pendente_gravando_a_data() {
        val id = UUID.randomUUID()
        val momento = LocalDateTime.of(2026, 4, 10, 14, 30)
        given(repository.findById(id)).willReturn(Optional.of(conta(id = id)))
        given(repository.save(any<ContaPagarEntity>())).willAnswer { it.arguments[0] }

        val resultado = service.quitar(id, momento)

        assertEquals(StatusContaType.PAGO, resultado.status)
        assertEquals(momento, resultado.pagoEm)
    }

    @Test
    fun deve_quitar_conta_atrasada() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(conta(id = id, status = StatusContaType.ATRASADO)))
        given(repository.save(any<ContaPagarEntity>())).willAnswer { it.arguments[0] }

        assertEquals(StatusContaType.PAGO, service.quitar(id, LocalDateTime.now()).status)
    }

    @Test
    fun deve_recusar_quitar_conta_ja_paga() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(
            Optional.of(conta(id = id, status = StatusContaType.PAGO, pagoEm = LocalDateTime.now().minusDays(1)))
        )

        assertThrows(ContaJaQuitadaException::class.java) { service.quitar(id, LocalDateTime.now()) }
        verify(repository, never()).save(any<ContaPagarEntity>())
    }

    @Test
    fun quitar_conta_inexistente_falha() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.empty())

        assertThrows(ContaPagarNaoEncontradaException::class.java) { service.quitar(id, LocalDateTime.now()) }
    }

    // --- edição e remoção ---

    @Test
    fun deve_atualizar_os_dados_da_despesa() {
        val existente = conta(status = StatusContaType.PENDENTE)
        given(repository.findById(existente.id!!)).willReturn(Optional.of(existente))
        given(repository.save(any<ContaPagarEntity>())).willAnswer { it.arguments[0] }

        val resultado = service.atualizar(
            existente.id!!, "Aluguel corrigido", CategoriaDespesaType.OUTROS,
            BigDecimal("3000.00"), LocalDate.now().plusDays(20)
        )

        assertEquals("Aluguel corrigido", resultado.descricao)
        assertEquals(CategoriaDespesaType.OUTROS, resultado.categoria)
        assertEquals(BigDecimal("3000.00"), resultado.valor)
    }

    /**
     * O ponto da edição: corrigir o vencimento de uma despesa lançada errada tem de tirá-la de
     * ATRASADO. Preservar o status antigo deixaria a conta marcada como atrasada para sempre.
     */
    @Test
    fun deve_recalcular_status_ao_adiar_despesa_atrasada() {
        val atrasada = conta(status = StatusContaType.ATRASADO)
        given(repository.findById(atrasada.id!!)).willReturn(Optional.of(atrasada))
        given(repository.save(any<ContaPagarEntity>())).willAnswer { it.arguments[0] }

        val resultado = service.atualizar(
            atrasada.id!!, "Aluguel", CategoriaDespesaType.ALUGUEL,
            BigDecimal("2500.00"), LocalDate.now().plusDays(10)
        )

        assertEquals(StatusContaType.PENDENTE, resultado.status)
    }

    @Test
    fun deve_marcar_como_atrasada_ao_editar_para_vencimento_passado() {
        val pendente = conta(status = StatusContaType.PENDENTE)
        given(repository.findById(pendente.id!!)).willReturn(Optional.of(pendente))
        given(repository.save(any<ContaPagarEntity>())).willAnswer { it.arguments[0] }

        val resultado = service.atualizar(
            pendente.id!!, "Aluguel", CategoriaDespesaType.ALUGUEL,
            BigDecimal("2500.00"), LocalDate.now().minusDays(1)
        )

        assertEquals(StatusContaType.ATRASADO, resultado.status)
    }

    @Test
    fun nao_deve_editar_despesa_ja_quitada() {
        val paga = conta(status = StatusContaType.PAGO, pagoEm = LocalDateTime.now())
        given(repository.findById(paga.id!!)).willReturn(Optional.of(paga))

        assertThrows(ContaJaQuitadaException::class.java) {
            service.atualizar(
                paga.id!!, "Outra coisa", CategoriaDespesaType.OUTROS,
                BigDecimal("1.00"), LocalDate.now().plusDays(1)
            )
        }
        verify(repository, never()).save(any<ContaPagarEntity>())
    }

    @Test
    fun deve_recusar_valor_zerado_na_edicao() {
        val existente = conta()
        given(repository.findById(existente.id!!)).willReturn(Optional.of(existente))

        assertThrows(ValorInvalidoException::class.java) {
            service.atualizar(
                existente.id!!, "Aluguel", CategoriaDespesaType.ALUGUEL,
                BigDecimal.ZERO, LocalDate.now().plusDays(1)
            )
        }
    }

    @Test
    fun deve_recusar_descricao_em_branco_na_edicao() {
        val existente = conta()
        given(repository.findById(existente.id!!)).willReturn(Optional.of(existente))

        assertThrows(DescricaoObrigatoriaException::class.java) {
            service.atualizar(
                existente.id!!, "   ", CategoriaDespesaType.ALUGUEL,
                BigDecimal("10.00"), LocalDate.now().plusDays(1)
            )
        }
    }

    @Test
    fun deve_remover_despesa_nao_quitada() {
        val existente = conta()
        given(repository.findById(existente.id!!)).willReturn(Optional.of(existente))

        service.remover(existente.id!!)

        verify(repository).delete(existente)
    }

    /** Despesa paga é caixa realizado — apagá-la reescreveria um mês já fechado. */
    @Test
    fun nao_deve_remover_despesa_quitada() {
        val paga = conta(status = StatusContaType.PAGO, pagoEm = LocalDateTime.now())
        given(repository.findById(paga.id!!)).willReturn(Optional.of(paga))

        assertThrows(ContaJaQuitadaException::class.java) { service.remover(paga.id!!) }
        verify(repository, never()).delete(any<ContaPagarEntity>())
    }

    @Test
    fun deve_lancar_excecao_ao_remover_despesa_inexistente() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.empty())

        assertThrows(ContaPagarNaoEncontradaException::class.java) { service.remover(id) }
    }
}
