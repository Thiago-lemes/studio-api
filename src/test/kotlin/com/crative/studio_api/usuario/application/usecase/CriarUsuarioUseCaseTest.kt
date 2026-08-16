package com.crative.studio_api.usuario.application.usecase

import com.crative.studio_api.usuario.domain.RoleType
import com.crative.studio_api.usuario.domain.UsuarioDomain
import com.crative.studio_api.usuario.domain.UsuarioRepository
import com.crative.studio_api.usuario.domain.exception.EmailJaExisteNaBaseException
import com.crative.studio_api.usuario.domain.exception.ProfessorNaoDeveSerCadastradoNesseFluxoException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.check
import org.springframework.security.crypto.password.PasswordEncoder


@ExtendWith(MockitoExtension::class)

class CriarUsuarioUseCaseTest {

    @Mock
    lateinit var usuarioRepository: UsuarioRepository

    @Mock
    lateinit var passwordEncoder: PasswordEncoder

    private lateinit var useCase: CriarUsuarioUseCase

    @BeforeEach
    fun setUp() {
        useCase = CriarUsuarioUseCase(usuarioRepository, passwordEncoder)
    }

    @Test
    fun deve_lancar_excecao_quando_email_ja_cadastrado() {

        val usuarioExistente = UsuarioDomain.criar(
            nome = "teste",
            email = "duplicado@studio.com",
            senhaHash = "teste",
            role = RoleType.ADMIN,
            professorId = null
        )

        given(usuarioRepository.buscarPorEmail("duplicado@studio.com")).willReturn(usuarioExistente)

        assertThrows(EmailJaExisteNaBaseException::class.java) {
            useCase.cadastrarUsuario(
                nome = "Outro Nome",
                email = "duplicado@studio.com",
                senha = "qualquerSenha",
                role = RoleType.SECRETARIA
            )
        }
    }

    @Test
    fun deve_lancar_excecao_de_professor_nesse_fluxo() {
        assertThrows(ProfessorNaoDeveSerCadastradoNesseFluxoException::class.java) {
            useCase.cadastrarUsuario(
                nome = "Outro Nome",
                email = "duplicado@studio.com",
                senha = "qualquerSenha",
                role = RoleType.PROFESSOR
            )
        }
    }

    @Test
    fun deve_criar_e_salvar_usuario_com_dados_corretos() {
        given(usuarioRepository.buscarPorEmail("novo@studio.com"))
            .willReturn(null)

        given(passwordEncoder.encode("senha123"))
            .willReturn("hash123")

        useCase.cadastrarUsuario(
            nome = "Ana Secretária",
            email = "novo@studio.com",
            senha = "senha123",
            role = RoleType.SECRETARIA
        )

        verify(usuarioRepository).salvar(
            check { usuario ->
                assertEquals("Ana Secretária", usuario.nome)
                assertEquals("novo@studio.com", usuario.email)
                assertEquals(RoleType.SECRETARIA, usuario.role)
                assertEquals("hash123", usuario.senhaHash)
            }
        )

        verify(passwordEncoder).encode("senha123")
    }
}