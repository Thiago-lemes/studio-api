package com.crative.studio_api.usuario.service

import com.crative.studio_api.shared.security.JwtService
import com.crative.studio_api.usuario.entity.RoleType
import com.crative.studio_api.usuario.entity.UsuarioEntity
import com.crative.studio_api.usuario.exception.CredenciaisInvalidasException
import com.crative.studio_api.usuario.exception.NovaSenhaIgualAAtualException
import com.crative.studio_api.usuario.exception.SenhaAtualIncorretaException
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
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional
import java.util.UUID

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

    // --- troca de senha ---

    private fun usuarioSalvo(ativo: Boolean = true) = UsuarioEntity(
        nome = "Ana", email = "ana@studio.com", senhaHash = "hashAtual",
        role = RoleType.SECRETARIA, ativo = ativo
    )

    @Test
    fun deve_trocar_a_senha_gravando_o_novo_hash() {
        val usuario = usuarioSalvo()
        given(usuarioRepository.findById(usuario.id)).willReturn(Optional.of(usuario))
        given(passwordEncoder.matches("senhaAtual", "hashAtual")).willReturn(true)
        given(passwordEncoder.matches("senhaNova", "hashAtual")).willReturn(false)
        given(passwordEncoder.encode("senhaNova")).willReturn("hashNovo")

        service.trocarSenha(usuario.id, "senhaAtual", "senhaNova")

        verify(usuarioRepository).save(check { assertEquals("hashNovo", it.senhaHash) })
    }

    /** 400, não 401: quem erra a digitação está autenticado — deslogar seria punir o engano. */
    @Test
    fun deve_recusar_troca_com_senha_atual_incorreta() {
        val usuario = usuarioSalvo()
        given(usuarioRepository.findById(usuario.id)).willReturn(Optional.of(usuario))
        given(passwordEncoder.matches("errada", "hashAtual")).willReturn(false)

        assertThrows(SenhaAtualIncorretaException::class.java) {
            service.trocarSenha(usuario.id, "errada", "senhaNova")
        }
        verify(usuarioRepository, never()).save(any())
    }

    @Test
    fun deve_recusar_nova_senha_igual_a_atual() {
        val usuario = usuarioSalvo()
        given(usuarioRepository.findById(usuario.id)).willReturn(Optional.of(usuario))
        given(passwordEncoder.matches("senhaAtual", "hashAtual")).willReturn(true)

        assertThrows(NovaSenhaIgualAAtualException::class.java) {
            service.trocarSenha(usuario.id, "senhaAtual", "senhaAtual")
        }
        verify(usuarioRepository, never()).save(any())
    }

    /** Token ainda válido de quem foi desligado no meio da sessão não pode mudar a senha. */
    @Test
    fun deve_recusar_troca_de_usuario_inativo() {
        val usuario = usuarioSalvo(ativo = false)
        given(usuarioRepository.findById(usuario.id)).willReturn(Optional.of(usuario))

        assertThrows(CredenciaisInvalidasException::class.java) {
            service.trocarSenha(usuario.id, "senhaAtual", "senhaNova")
        }
    }

    @Test
    fun deve_lancar_excecao_ao_trocar_senha_de_usuario_inexistente() {
        val id = UUID.randomUUID()
        given(usuarioRepository.findById(id)).willReturn(Optional.empty())

        assertThrows(UsuarioNaoEncontradoException::class.java) {
            service.trocarSenha(id, "a", "b")
        }
    }
}
