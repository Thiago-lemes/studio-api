package com.crative.studio_api.aluno.service

import com.crative.studio_api.aluno.entity.AlunoEntity
import com.crative.studio_api.aluno.exception.AlunoNaoEncontradoException
import com.crative.studio_api.aluno.exception.CpfJaCadastradoException
import com.crative.studio_api.aluno.exception.DataNascimentoFuturaException
import com.crative.studio_api.aluno.repository.AlunoRepository
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
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AlunoServiceTest {

    @Mock
    lateinit var repository: AlunoRepository

    private lateinit var service: AlunoService

    @BeforeEach
    fun setUp() {
        service = AlunoService(repository)
    }

    private fun aluno(
        id: UUID? = UUID.randomUUID(),
        nome: String = "Joana",
        dataNascimento: LocalDate = LocalDate.now().minusYears(20),
        cpf: String? = "111.111.111-11",
        ativo: Boolean = true
    ) = AlunoEntity(
        id = id,
        nome = nome,
        telefone = "11988887777",
        dataNascimento = dataNascimento,
        cpf = cpf,
        ativo = ativo
    )

    @Test
    fun deve_cadastrar_aluno_calculando_a_idade() {
        val nascimento = LocalDate.now().minusYears(30)
        given(repository.findByCpf("111.111.111-11")).willReturn(null)
        given(repository.save(any<AlunoEntity>())).willAnswer { it.arguments[0] }

        val resultado = service.cadastrar("Joana", "11988887777", nascimento, "111.111.111-11")

        assertEquals(30, resultado.idade)
        assertFalse(resultado.menorDeIdade)
        verify(repository).save(check {
            assertEquals("Joana", it.nome)
            assertEquals("111.111.111-11", it.cpf)
            assertTrue(it.ativo)
        })
    }

    @Test
    fun deve_marcar_como_menor_de_idade_quem_tem_menos_de_dezoito() {
        val nascimento = LocalDate.now().minusYears(10)
        given(repository.findByCpf(any())).willReturn(null)
        given(repository.save(any<AlunoEntity>())).willAnswer { it.arguments[0] }

        val resultado = service.cadastrar("Pedrinho", null, nascimento, "222.222.222-22")

        assertEquals(10, resultado.idade)
        assertTrue(resultado.menorDeIdade)
    }

    @Test
    fun quem_faz_dezoito_hoje_nao_e_menor_de_idade() {
        given(repository.findByCpf(any())).willReturn(null)
        given(repository.save(any<AlunoEntity>())).willAnswer { it.arguments[0] }

        val resultado = service.cadastrar("Limite", null, LocalDate.now().minusYears(18), "333.333.333-33")

        assertEquals(18, resultado.idade)
        assertFalse(resultado.menorDeIdade)
    }

    @Test
    fun quem_faz_dezoito_amanha_ainda_e_menor_de_idade() {
        given(repository.findByCpf(any())).willReturn(null)
        given(repository.save(any<AlunoEntity>())).willAnswer { it.arguments[0] }

        val nascimento = LocalDate.now().minusYears(18).plusDays(1)
        val resultado = service.cadastrar("Quase", null, nascimento, "444.444.444-44")

        assertEquals(17, resultado.idade)
        assertTrue(resultado.menorDeIdade)
    }

    @Test
    fun deve_recusar_data_de_nascimento_futura() {
        assertThrows(DataNascimentoFuturaException::class.java) {
            service.cadastrar("Futuro", null, LocalDate.now().plusDays(1), "555.555.555-55")
        }
        verify(repository, never()).save(any<AlunoEntity>())
    }

    @Test
    fun deve_aceitar_nascimento_hoje() {
        given(repository.findByCpf(any())).willReturn(null)
        given(repository.save(any<AlunoEntity>())).willAnswer { it.arguments[0] }

        val resultado = service.cadastrar("Recém nascido", null, LocalDate.now(), "666.666.666-66")

        assertEquals(0, resultado.idade)
    }

    @Test
    fun deve_recusar_cpf_ja_cadastrado() {
        given(repository.findByCpf("111.111.111-11")).willReturn(aluno())

        assertThrows(CpfJaCadastradoException::class.java) {
            service.cadastrar("Outra", null, LocalDate.now().minusYears(20), "111.111.111-11")
        }
        verify(repository, never()).save(any<AlunoEntity>())
    }

    @Test
    fun deve_buscar_aluno_por_id() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(aluno(id = id, nome = "Joana")))

        assertEquals("Joana", service.buscarPorId(id).aluno.nome)
    }

    @Test
    fun deve_lancar_excecao_ao_buscar_id_inexistente() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.empty())

        assertThrows(AlunoNaoEncontradoException::class.java) { service.buscarPorId(id) }
    }

    @Test
    fun deve_listar_apenas_os_ativos() {
        given(repository.findAllByAtivoTrue()).willReturn(listOf(aluno(nome = "A"), aluno(nome = "B")))

        val resultado = service.listarAtivos()

        assertEquals(listOf("A", "B"), resultado.map { it.aluno.nome })
    }

    @Test
    fun deve_atualizar_nome_e_telefone_sem_tocar_em_cpf_e_nascimento() {
        val id = UUID.randomUUID()
        val existente = aluno(id = id, nome = "Nome Antigo")
        given(repository.findById(id)).willReturn(Optional.of(existente))
        given(repository.save(any<AlunoEntity>())).willAnswer { it.arguments[0] }

        val resultado = service.atualizar(id, "Nome Novo", "11900000000")

        assertEquals("Nome Novo", resultado.aluno.nome)
        assertEquals("11900000000", resultado.aluno.telefone)
        assertEquals("111.111.111-11", resultado.aluno.cpf)
    }

    @Test
    fun deve_lancar_excecao_ao_atualizar_aluno_inexistente() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.empty())

        assertThrows(AlunoNaoEncontradoException::class.java) {
            service.atualizar(id, "Nome", null)
        }
    }

    @Test
    fun deve_inativar_o_aluno_sem_apagar_o_registro() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(aluno(id = id)))
        given(repository.save(any<AlunoEntity>())).willAnswer { it.arguments[0] }

        val resultado = service.alterarStatus(id, ativo = false)

        assertFalse(resultado.aluno.ativo)
        verify(repository).save(check { assertFalse(it.ativo) })
        verify(repository, never()).delete(any())
    }

    @Test
    fun deve_reativar_o_aluno() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(aluno(id = id, ativo = false)))
        given(repository.save(any<AlunoEntity>())).willAnswer { it.arguments[0] }

        assertTrue(service.alterarStatus(id, ativo = true).aluno.ativo)
    }

    // --- busca por nome e por responsável ---

    @Test
    fun sem_filtro_devolve_todos_os_ativos_ordenados_por_nome() {
        given(repository.findAllByAtivoTrue()).willReturn(listOf(aluno(nome = "Zeca"), aluno(nome = "Ana")))

        assertEquals(listOf("Ana", "Zeca"), service.listarAtivos(null, null).map { it.aluno.nome })
    }

    @Test
    fun deve_buscar_por_trecho_do_nome() {
        given(repository.findAllByAtivoTrueAndNomeContainingIgnoreCase("ana"))
            .willReturn(listOf(aluno(nome = "Ana")))

        assertEquals(1, service.listarAtivos("ana", null).size)
    }

    /** O caso real: a secretária tem o nome de quem ligou, não o do aluno. */
    @Test
    fun deve_buscar_aluno_pelo_responsavel() {
        given(repository.buscarAtivosPorResponsavel("Silva")).willReturn(listOf(aluno(nome = "Joana")))

        assertEquals("Joana", service.listarAtivos(null, "Silva").single().aluno.nome)
    }

    /** Combináveis, não exclusivos: os dois termos precisam valer ao mesmo tempo. */
    @Test
    fun deve_combinar_nome_do_aluno_com_nome_do_responsavel() {
        given(repository.buscarAtivosPorResponsavel("Silva"))
            .willReturn(listOf(aluno(nome = "Ana"), aluno(nome = "Bruno")))

        val resultado = service.listarAtivos("ana", "Silva")

        assertEquals(listOf("Ana"), resultado.map { it.aluno.nome })
        verify(repository, never()).findAllByAtivoTrueAndNomeContainingIgnoreCase(any())
    }

    @Test
    fun termo_em_branco_e_tratado_como_ausencia_de_filtro() {
        given(repository.findAllByAtivoTrue()).willReturn(listOf(aluno(nome = "Ana")))

        assertEquals(1, service.listarAtivos("  ", "").size)
        verify(repository, never()).buscarAtivosPorResponsavel(any())
    }

    @Test
    fun busca_por_nome_ignora_maiusculas_ao_combinar_com_responsavel() {
        given(repository.buscarAtivosPorResponsavel("Silva")).willReturn(listOf(aluno(nome = "JOANA")))

        assertEquals(1, service.listarAtivos("joana", "Silva").size)
    }
}
