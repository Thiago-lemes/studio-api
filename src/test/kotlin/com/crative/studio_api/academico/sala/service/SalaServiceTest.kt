package com.crative.studio_api.academico.sala.service

import com.crative.studio_api.academico.exception.CapacidadeObrigatoriaException
import com.crative.studio_api.academico.exception.NomeSalaObrigatorioException
import com.crative.studio_api.academico.exception.SalaNaoEncontradaException
import com.crative.studio_api.academico.exception.SalaJaCadastradaException
import com.crative.studio_api.academico.sala.entity.SalaEntity
import com.crative.studio_api.academico.sala.repository.SalaRepository
import org.junit.jupiter.api.Assertions.assertEquals
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
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SalaServiceTest {

    @Mock
    lateinit var repository: SalaRepository

    private lateinit var service: SalaService

    @BeforeEach
    fun setUp() {
        service = SalaService(repository)
    }

    private fun sala(id: UUID? = UUID.randomUUID(), nome: String = "Sala 1", capacidade: Int = 20) =
        SalaEntity(id = id, nome = nome, capacidade = capacidade)

    @Test
    fun deve_cadastrar_sala() {
        given(repository.findByNome("Sala 1")).willReturn(null)
        given(repository.save(any<SalaEntity>())).willAnswer { it.arguments[0] }

        val resultado = service.cadastrar("Sala 1", 20)

        assertEquals("Sala 1", resultado.nome)
        assertEquals(20, resultado.capacidade)
    }

    @Test
    fun deve_recusar_nome_duplicado() {
        given(repository.findByNome("Sala 1")).willReturn(sala())

        assertThrows(SalaJaCadastradaException::class.java) { service.cadastrar("Sala 1", 20) }
        verify(repository, never()).save(any<SalaEntity>())
    }

    @Test
    fun deve_recusar_nome_em_branco_antes_de_consultar_a_base() {
        assertThrows(NomeSalaObrigatorioException::class.java) { service.cadastrar("   ", 20) }

        verify(repository, never()).findByNome(any())
        verify(repository, never()).save(any<SalaEntity>())
    }

    @Test
    fun deve_recusar_capacidade_zero() {
        assertThrows(CapacidadeObrigatoriaException::class.java) { service.cadastrar("Sala 1", 0) }
    }

    @Test
    fun deve_recusar_capacidade_negativa() {
        assertThrows(CapacidadeObrigatoriaException::class.java) { service.cadastrar("Sala 1", -5) }
    }

    @Test
    fun deve_buscar_sala_por_id() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(sala(id = id)))

        assertEquals(id, service.buscarPorId(id).id)
    }

    @Test
    fun deve_lancar_excecao_ao_buscar_id_inexistente() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.empty())

        assertThrows(SalaNaoEncontradaException::class.java) { service.buscarPorId(id) }
    }

    @Test
    fun deve_listar_todas_as_salas() {
        given(repository.findAll()).willReturn(listOf(sala(nome = "Sala 1"), sala(nome = "Sala 2")))

        assertEquals(listOf("Sala 1", "Sala 2"), service.listarSalas().map { it.nome })
    }

    @Test
    fun deve_atualizar_sala_ao_trocar_o_nome() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(sala(id = id, nome = "Sala 1")))
        given(repository.findByNome("Sala 2")).willReturn(null)
        given(repository.save(any<SalaEntity>())).willAnswer { it.arguments[0] }

        val resultado = service.atualizarSala(id, "Sala 2", 30)

        assertEquals("Sala 2", resultado.nome)
        assertEquals(30, resultado.capacidade)
    }

    /** A sala editada não é duplicata de si mesma — este é o caminho de "mudar só a capacidade". */
    @Test
    fun deve_atualizar_a_capacidade_mantendo_o_proprio_nome() {
        val id = UUID.randomUUID()
        val existente = sala(id = id, nome = "Sala 1")
        given(repository.findById(id)).willReturn(Optional.of(existente))
        given(repository.findByNome("Sala 1")).willReturn(existente)
        given(repository.save(any<SalaEntity>())).willAnswer { it.arguments[0] }

        val resultado = service.atualizarSala(id, "Sala 1", 40)

        assertEquals("Sala 1", resultado.nome)
        assertEquals(40, resultado.capacidade)
    }

    @Test
    fun deve_recusar_atualizacao_para_o_nome_de_outra_sala() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(sala(id = id, nome = "Sala 1")))
        given(repository.findByNome("Sala 2")).willReturn(sala(id = UUID.randomUUID(), nome = "Sala 2"))

        assertThrows(SalaJaCadastradaException::class.java) {
            service.atualizarSala(id, "Sala 2", 40)
        }
        verify(repository, never()).save(any<SalaEntity>())
    }

    @Test
    fun atualizar_com_capacidade_invalida_falha_sem_salvar() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(sala(id = id)))

        assertThrows(CapacidadeObrigatoriaException::class.java) {
            service.atualizarSala(id, "Sala 1", 0)
        }
        verify(repository, never()).save(any<SalaEntity>())
    }

    @Test
    fun deve_lancar_excecao_ao_atualizar_sala_inexistente() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.empty())

        assertThrows(SalaNaoEncontradaException::class.java) { service.atualizarSala(id, "Sala 9", 10) }
    }

    @Test
    fun deve_deletar_a_sala() {
        val id = UUID.randomUUID()
        val existente = sala(id = id)
        given(repository.findById(id)).willReturn(Optional.of(existente))

        service.deletarSala(id)

        verify(repository).delete(existente)
    }

    @Test
    fun deve_lancar_excecao_ao_deletar_sala_inexistente() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.empty())

        assertThrows(SalaNaoEncontradaException::class.java) { service.deletarSala(id) }
        verify(repository, never()).delete(any<SalaEntity>())
    }
}
