package com.crative.studio_api.usuario.application.usecase

import com.crative.studio_api.shared.security.JwtService
import com.crative.studio_api.usuario.domain.RoleType
import com.crative.studio_api.usuario.domain.UsuarioDomain
import com.crative.studio_api.usuario.domain.UsuarioRepository
import com.crative.studio_api.usuario.domain.exception.CredenciaisInvalidasException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@ExtendWith(MockitoExtension::class)
class AutenticarUsuarioUseCaseTest {

    @Mock
    lateinit var usuarioRepository: UsuarioRepository

    @Mock
    lateinit var jwtService: JwtService

    private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()

    private lateinit var useCase: AutenticarUsuarioUseCase

    @BeforeEach
    fun setUp() {
        useCase = AutenticarUsuarioUseCase(usuarioRepository, jwtService, passwordEncoder)
    }

    @Test
    fun deve_autenticar_e_gerar_token_com_credenciais_corretas() {
        val senhaHash = passwordEncoder.encode("senha123")
        val usuario = UsuarioDomain.criar(
            nome = "Ana",
            email = "ana@studio.com",
            senhaHash = senhaHash!!,
            role = RoleType.SECRETARIA,
            professorId = null
        )

        given(usuarioRepository.buscarPorEmail("ana@studio.com")).willReturn(usuario)
        given(jwtService.gerarToken(usuario.id, usuario.role, usuario.professorId))
            .willReturn("token-fake")

        val resultado = useCase.autenticar("ana@studio.com", "senha123")

        assertEquals("token-fake", resultado.token)
        assertEquals(usuario.id, resultado.usuario.id)
    }

    @Test
    fun deve_lancar_excecao_com_senha_incorreta() {
        val senhaHash = passwordEncoder.encode("senhaCorreta")
        val usuario = UsuarioDomain.criar(
            nome = "Ana",
            email = "ana@studio.com",
            senhaHash = senhaHash!!,
            role = RoleType.SECRETARIA,
            professorId = null
        )

        given(usuarioRepository.buscarPorEmail("ana@studio.com")).willReturn(usuario)

        assertThrows(CredenciaisInvalidasException::class.java) {
            useCase.autenticar("ana@studio.com", "senhaErrada")
        }
    }

    @Test
    fun deve_lancar_excecao_quando_email_nao_existe() {
        given(usuarioRepository.buscarPorEmail("naoexiste@studio.com")).willReturn(null)

        assertThrows(CredenciaisInvalidasException::class.java) {
            useCase.autenticar("naoexiste@studio.com", "qualquerSenha")
        }
    }

    @Test
    fun deve_lancar_excecao_quando_usuario_esta_inativo() {
        val senhaHash = passwordEncoder.encode("senha123")

        val usuario = UsuarioDomain.criar(
            nome = "Ana",
            email = "ana@studio.com",
            senhaHash = senhaHash!!,
            role = RoleType.SECRETARIA,
            professorId = null
        ).copy(ativo = false)

        given(usuarioRepository.buscarPorEmail("ana@studio.com")).willReturn(usuario)

        assertThrows(CredenciaisInvalidasException::class.java) {
            useCase.autenticar("ana@studio.com", "senha123")
        }
    }
}