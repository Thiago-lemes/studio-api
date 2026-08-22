package com.crative.studio_api.usuario.service

import com.crative.studio_api.shared.security.JwtService
import com.crative.studio_api.usuario.entity.RoleType
import com.crative.studio_api.usuario.entity.UsuarioEntity
import com.crative.studio_api.usuario.exception.CredenciaisInvalidasException
import com.crative.studio_api.usuario.repository.UsuarioRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.password.PasswordEncoder

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock
    lateinit var usuarioRepository: UsuarioRepository

    @Mock
    lateinit var jwtService: JwtService

    @Mock
    lateinit var passwordEncoder: PasswordEncoder

    private lateinit var service: AuthService

    @BeforeEach
    fun setUp() {
        service = AuthService(usuarioRepository, jwtService, passwordEncoder)
    }

    @Test
    fun deve_autenticar_e_gerar_token_com_credenciais_corretas() {
        val usuario = UsuarioEntity(
            nome = "Ana", email = "ana@studio.com", senhaHash = "hashSalvo",
            role = RoleType.SECRETARIA
        )
        given(usuarioRepository.findByEmail("ana@studio.com")).willReturn(usuario)
        given(
            passwordEncoder.matches(
                "senha123",
                "hashSalvo"
            )
        ).willReturn(true)
        given(
            jwtService.gerarToken(
                usuario.id, usuario.role,
                usuario.professorId
            )
        ).willReturn("token-fake")

        val resultado = service.autenticar("ana@studio.com", "senha123")

        assertEquals("token-fake", resultado.token)
        assertEquals(usuario.id, resultado.usuario.id)
    }

    @Test
    fun deve_lancar_excecao_com_senha_incorreta() {
        val usuario =
            UsuarioEntity(nome = "Ana", email = "ana@studio.com", senhaHash = "hashSalvo", role = RoleType.SECRETARIA)
        given(usuarioRepository.findByEmail("ana@studio.com")).willReturn(usuario)
        given(passwordEncoder.matches("senhaErrada", "hashSalvo"))
            .willReturn(false)

        assertThrows(CredenciaisInvalidasException::class.java) {
            service.autenticar("ana@studio.com", "senhaErrada")
        }
    }

    @Test
    fun deve_lancar_excecao_quando_email_nao_existe() {
        given(usuarioRepository.findByEmail("naoexiste@studio.com")).willReturn(null)

        assertThrows(CredenciaisInvalidasException::class.java) {
            service.autenticar("naoexiste@studio.com", "qualquer")
        }
    }

    @Test
    fun deve_lancar_excecao_quando_usuario_esta_inativo() {
        val usuario = UsuarioEntity(
            nome = "Ana", email = "ana@studio.com", senhaHash = "hashSalvo",
            role = RoleType.SECRETARIA, ativo = false
        )
        given(usuarioRepository.findByEmail("ana@studio.com")).willReturn(usuario)

        assertThrows(CredenciaisInvalidasException::class.java) {
            service.autenticar("ana@studio.com", "qualquer")
        }
    }
}