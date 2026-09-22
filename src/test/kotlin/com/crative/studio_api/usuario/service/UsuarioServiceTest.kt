package com.crative.studio_api.usuario.service

import com.crative.studio_api.usuario.entity.RoleType
import com.crative.studio_api.usuario.entity.UsuarioEntity
import com.crative.studio_api.usuario.exception.AutoInativacaoNaoPermitidaException
import com.crative.studio_api.usuario.exception.EmailJaExisteNaBaseException
import com.crative.studio_api.usuario.exception.ProfessorIdNaoPermitidoException
import com.crative.studio_api.usuario.exception.ProfessorIdObrigatorioException
import com.crative.studio_api.usuario.exception.UltimoAdminAtivoException
import com.crative.studio_api.usuario.exception.UsuarioNaoEncontradoException
import com.crative.studio_api.usuario.repository.UsuarioRepository
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
import org.mockito.kotlin.check
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UsuarioServiceTest {

    @Mock
    lateinit var repository: UsuarioRepository

    @Mock
    lateinit var passwordEncoder: PasswordEncoder

    private lateinit var service: UsuarioService

    @BeforeEach
    fun setUp() {
        service = UsuarioService(repository, passwordEncoder)
    }

    @Test
    fun deve_lancar_excecao_ao_cadastrar_professor_sem_professorId() {
        assertThrows(ProfessorIdObrigatorioException::class.java) {
            service.cadastrarUsuario("Ana", "ana@studio.com", "senha123", RoleType.PROFESSOR)
        }
    }

    @Test
    fun deve_lancar_excecao_ao_cadastrar_nao_professor_com_professorId() {
        assertThrows(ProfessorIdNaoPermitidoException::class.java) {
            service.cadastrarUsuario("Ana", "ana@studio.com", "senha123", RoleType.SECRETARIA, UUID.randomUUID())
        }
    }

    /** No fluxo de convite PROFESSOR é papel legítimo — o bloqueio antigo saiu junto do POST /usuarios. */
    @Test
    fun deve_cadastrar_professor_com_professorId() {
        val professorId = UUID.randomUUID()
        given(repository.findByEmail("prof@studio.com")).willReturn(null)
        given(passwordEncoder.encode("senha123")).willReturn("hash123")
        given(repository.save(org.mockito.kotlin.any())).willAnswer { it.arguments[0] }

        val resultado = service.cadastrarUsuario("Prof", "prof@studio.com", "senha123", RoleType.PROFESSOR, professorId)

        assertEquals(RoleType.PROFESSOR, resultado.role)
        assertEquals(professorId, resultado.professorId)
    }

    @Test
    fun deve_lancar_excecao_quando_email_ja_cadastrado() {
        val existente = UsuarioEntity(
            nome = "Ana",
            email = "duplicado@studio.com",
            senhaHash = "hash",
            role = RoleType.ADMIN
        )
        given(repository.findByEmail("duplicado@studio.com")).willReturn(existente)

        assertThrows(EmailJaExisteNaBaseException::class.java) {
            service.cadastrarUsuario("Outra", "duplicado@studio.com", "senha", RoleType.SECRETARIA)
        }
    }

    @Test
    fun deve_criar_e_salvar_usuario_com_dados_corretos() {
        given(repository.findByEmail("novo@studio.com")).willReturn(null)
        given(passwordEncoder.encode("senha123")).willReturn("hash123")
        given(repository.save(org.mockito.kotlin.any())).willAnswer { it.arguments[0] }

        val resultado = service.cadastrarUsuario("Ana Secretária", "novo@studio.com", "senha123", RoleType.SECRETARIA)

        assertEquals("Ana Secretária", resultado.nome)
        assertEquals("hash123", resultado.senhaHash)

        verify(repository).save(check {
            assertEquals("novo@studio.com", it.email)
            assertEquals(RoleType.SECRETARIA, it.role)
        })
    }

    @Test
    fun deve_retornar_usuario_ao_buscar_por_id_existente() {
        val id = UUID.randomUUID()
        val usuario = UsuarioEntity(id = id, nome = "Ana", email = "ana@studio.com", senhaHash = "hash", role = RoleType.ADMIN)
        given(repository.findById(id)).willReturn(Optional.of(usuario))

        val resultado = service.buscarPorId(id)

        assertEquals(id, resultado.id)
    }

    @Test
    fun deve_lancar_excecao_ao_buscar_id_inexistente() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.empty())

        assertThrows(UsuarioNaoEncontradoException::class.java) {
            service.buscarPorId(id)
        }
    }

    private fun usuario(
        id: UUID = UUID.randomUUID(),
        nome: String = "Ana",
        email: String = "ana@studio.com",
        role: RoleType = RoleType.SECRETARIA,
        ativo: Boolean = true
    ) = UsuarioEntity(id = id, nome = nome, email = email, senhaHash = "hash", role = role, ativo = ativo)

    // --- listagem ---

    /** Inativos entram por padrão: sem eles, a tela de gestão nunca conseguiria reativar ninguém. */
    @Test
    fun deve_listar_todos_incluindo_inativos_quando_nao_ha_filtro() {
        val ativos = usuario(nome = "Bruno")
        val inativo = usuario(nome = "Ana", ativo = false)
        given(repository.findAll()).willReturn(listOf(ativos, inativo))

        val resultado = service.listar(role = null, ativo = null)

        assertEquals(2, resultado.size)
        assertEquals(listOf("Ana", "Bruno"), resultado.map { it.nome })
    }

    @Test
    fun deve_combinar_filtro_de_papel_e_status() {
        val esperado = usuario(role = RoleType.ADMIN)
        given(repository.findAllByRoleAndAtivo(RoleType.ADMIN, true)).willReturn(listOf(esperado))

        val resultado = service.listar(RoleType.ADMIN, ativo = true)

        assertEquals(1, resultado.size)
    }

    // --- alterar status ---

    @Test
    fun deve_inativar_usuario() {
        val alvo = usuario(role = RoleType.SECRETARIA)
        given(repository.findById(alvo.id)).willReturn(Optional.of(alvo))
        given(repository.save(org.mockito.kotlin.any())).willAnswer { it.arguments[0] }

        val resultado = service.alterarStatus(alvo.id, ativo = false, solicitanteId = UUID.randomUUID())

        assertFalse(resultado.ativo)
    }

    /** Idempotente: repetir o status atual não deve gravar nada. */
    @Test
    fun nao_deve_salvar_quando_status_ja_e_o_solicitado() {
        val alvo = usuario(ativo = true)
        given(repository.findById(alvo.id)).willReturn(Optional.of(alvo))

        val resultado = service.alterarStatus(alvo.id, ativo = true, solicitanteId = UUID.randomUUID())

        assertTrue(resultado.ativo)
        verify(repository, never()).save(org.mockito.kotlin.any())
    }

    @Test
    fun deve_recusar_que_o_usuario_inative_o_proprio_acesso() {
        val alvo = usuario(role = RoleType.ADMIN)
        given(repository.findById(alvo.id)).willReturn(Optional.of(alvo))

        assertThrows(AutoInativacaoNaoPermitidaException::class.java) {
            service.alterarStatus(alvo.id, ativo = false, solicitanteId = alvo.id)
        }
        verify(repository, never()).save(org.mockito.kotlin.any())
    }

    @Test
    fun deve_recusar_inativacao_do_ultimo_admin_ativo() {
        val alvo = usuario(role = RoleType.ADMIN)
        given(repository.findById(alvo.id)).willReturn(Optional.of(alvo))
        given(repository.countByRoleAndAtivoTrue(RoleType.ADMIN)).willReturn(1)

        assertThrows(UltimoAdminAtivoException::class.java) {
            service.alterarStatus(alvo.id, ativo = false, solicitanteId = UUID.randomUUID())
        }
    }

    @Test
    fun deve_permitir_inativar_admin_quando_ha_outro_ativo() {
        val alvo = usuario(role = RoleType.ADMIN)
        given(repository.findById(alvo.id)).willReturn(Optional.of(alvo))
        given(repository.countByRoleAndAtivoTrue(RoleType.ADMIN)).willReturn(2)
        given(repository.save(org.mockito.kotlin.any())).willAnswer { it.arguments[0] }

        assertFalse(service.alterarStatus(alvo.id, ativo = false, solicitanteId = UUID.randomUUID()).ativo)
    }

    /** Reativar não passa pelas travas — elas só existem para não deixar o sistema sem administrador. */
    @Test
    fun deve_reativar_ultimo_admin_sem_travas() {
        val alvo = usuario(role = RoleType.ADMIN, ativo = false)
        given(repository.findById(alvo.id)).willReturn(Optional.of(alvo))
        given(repository.save(org.mockito.kotlin.any())).willAnswer { it.arguments[0] }

        assertTrue(service.alterarStatus(alvo.id, ativo = true, solicitanteId = alvo.id).ativo)
    }
}
