package com.crative.studio_api.usuario.service

import com.crative.studio_api.usuario.entity.RoleType
import com.crative.studio_api.usuario.entity.UsuarioEntity
import com.crative.studio_api.usuario.exception.EmailJaExisteNaBaseException
import com.crative.studio_api.usuario.exception.ProfessorNaoDeveSerCadastradoNesseFluxoException
import com.crative.studio_api.usuario.exception.UsuarioNaoEncontradoException
import com.crative.studio_api.usuario.repository.UsuarioRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.check
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
    fun deve_lancar_excecao_ao_tentar_cadastrar_role_professor() {
        assertThrows(ProfessorNaoDeveSerCadastradoNesseFluxoException::class.java) {
            service.cadastrarUsuario("Ana", "ana@studio.com", "senha123", RoleType.PROFESSOR)
        }
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
}