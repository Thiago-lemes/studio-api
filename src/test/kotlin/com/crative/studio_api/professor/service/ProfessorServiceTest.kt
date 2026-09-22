package com.crative.studio_api.professor.service

import com.crative.studio_api.professor.entity.ProfessorEntity
import com.crative.studio_api.professor.exception.NomeObrigatorioException
import com.crative.studio_api.professor.exception.ProfessorJaCadastradoException
import com.crative.studio_api.professor.exception.ProfessorNaoEncontradoException
import com.crative.studio_api.professor.repository.ProfessorRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ProfessorServiceTest {

    @Mock
    lateinit var repository: ProfessorRepository

    private lateinit var service: ProfessorService

    @BeforeEach
    fun setUp() {
        service = ProfessorService(repository)
    }

    private fun professor(
        id: UUID? = UUID.randomUUID(),
        nome: String = "Marina",
        telefone: String = "11999990000",
        ativo: Boolean = true
    ) = ProfessorEntity(
        id = id, nome = nome, telefone = telefone, especialidade = "Ballet", ativo = ativo
    )

    @Test
    fun deve_cadastrar_professor_ativo() {
        given(repository.findByNomeAndTelefone("Marina", "11999990000")).willReturn(null)
        given(repository.save(any<ProfessorEntity>())).willAnswer { it.arguments[0] }

        val resultado = service.cadastrar("Marina", "11999990000", "Ballet")

        assertEquals("Marina", resultado.nome)
        assertEquals("Ballet", resultado.especialidade)
        assertTrue(resultado.ativo)
    }

    @Test
    fun deve_aceitar_professor_sem_especialidade() {
        given(repository.findByNomeAndTelefone(any(), any())).willReturn(null)
        given(repository.save(any<ProfessorEntity>())).willAnswer { it.arguments[0] }

        assertEquals(null, service.cadastrar("Marina", "11999990000", null).especialidade)
    }

    @Test
    fun deve_recusar_nome_em_branco() {
        assertThrows(NomeObrigatorioException::class.java) {
            service.cadastrar("   ", "11999990000", null)
        }
        verify(repository, never()).save(any<ProfessorEntity>())
    }

    @Test
    fun deve_recusar_duplicado_por_nome_e_telefone() {
        given(repository.findByNomeAndTelefone("Marina", "11999990000")).willReturn(professor())

        assertThrows(ProfessorJaCadastradoException::class.java) {
            service.cadastrar("Marina", "11999990000", null)
        }
        verify(repository, never()).save(any<ProfessorEntity>())
    }

    @Test
    fun mesmo_nome_com_telefone_diferente_e_permitido() {
        given(repository.findByNomeAndTelefone("Marina", "11888887777")).willReturn(null)
        given(repository.save(any<ProfessorEntity>())).willAnswer { it.arguments[0] }

        assertEquals("11888887777", service.cadastrar("Marina", "11888887777", null).telefone)
    }

    @Test
    fun deve_buscar_por_id() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(professor(id = id)))

        assertEquals(id, service.buscarPorId(id).id)
    }

    @Test
    fun deve_lancar_excecao_ao_buscar_id_inexistente() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.empty())

        assertThrows(ProfessorNaoEncontradoException::class.java) { service.buscarPorId(id) }
    }

    @Test
    fun deve_listar_apenas_os_ativos() {
        given(repository.findAllByAtivoTrue()).willReturn(listOf(professor(nome = "A")))

        assertEquals(listOf("A"), service.listarAtivos().map { it.nome })
    }

    @Test
    fun deve_atualizar_os_dados_do_professor() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(professor(id = id)))
        given(repository.save(any<ProfessorEntity>())).willAnswer { it.arguments[0] }

        val resultado = service.atualizar(id, "Marina Costa", "11911112222", "Jazz")

        assertEquals("Marina Costa", resultado.nome)
        assertEquals("11911112222", resultado.telefone)
        assertEquals("Jazz", resultado.especialidade)
    }

    @Test
    fun atualizar_com_nome_em_branco_falha_antes_de_buscar_o_professor() {
        assertThrows(NomeObrigatorioException::class.java) {
            service.atualizar(UUID.randomUUID(), "", "11999990000", null)
        }
        verify(repository, never()).findById(any())
    }

    @Test
    fun deve_lancar_excecao_ao_atualizar_professor_inexistente() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.empty())

        assertThrows(ProfessorNaoEncontradoException::class.java) {
            service.atualizar(id, "Marina", "11999990000", null)
        }
    }

    @Test
    fun deve_inativar_e_reativar_o_professor() {
        val id = UUID.randomUUID()
        val entidade = professor(id = id)
        given(repository.findById(id)).willReturn(Optional.of(entidade))
        given(repository.save(any<ProfessorEntity>())).willAnswer { it.arguments[0] }

        assertFalse(service.alterarStatus(id, ativo = false).ativo)
        assertTrue(service.alterarStatus(id, ativo = true).ativo)
        verify(repository, never()).delete(any())
    }

    @Test
    fun alterar_status_de_professor_inexistente_falha() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.empty())

        assertThrows(ProfessorNaoEncontradoException::class.java) {
            service.alterarStatus(id, ativo = false)
        }
    }

    @Test
    fun deve_persistir_exatamente_os_dados_informados() {
        given(repository.findByNomeAndTelefone(any(), any())).willReturn(null)
        given(repository.save(any<ProfessorEntity>())).willAnswer { it.arguments[0] }

        service.cadastrar("Marina", "11999990000", "Ballet")

        verify(repository).save(check {
            assertEquals("Marina", it.nome)
            assertEquals("11999990000", it.telefone)
            assertEquals("Ballet", it.especialidade)
            assertTrue(it.ativo)
        })
    }
}
