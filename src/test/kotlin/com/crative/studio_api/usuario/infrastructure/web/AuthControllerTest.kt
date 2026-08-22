package com.crative.studio_api.usuario.infrastructure.web

import com.crative.studio_api.usuario.application.usecase.AutenticacaoResponse
import com.crative.studio_api.usuario.application.usecase.AutenticarUsuarioUseCase
import com.crative.studio_api.usuario.domain.RoleType
import com.crative.studio_api.usuario.domain.UsuarioDomain
import com.crative.studio_api.usuario.domain.exception.CredenciaisInvalidasException
import com.crative.studio_api.usuario.infrastructure.web.controller.AuthController
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AuthController::class)
@AutoConfigureMockMvc(addFilters = false)

class AuthControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var autenticarUsuarioUseCase: AutenticarUsuarioUseCase

    @Test
    fun deve_retornar_200_com_token_quando_login_correto() {
        val usuario = UsuarioDomain.criar(
            nome = "Ana",
            email = "ana@studio.com",
            senhaHash = "hash-qualquer",
            role = RoleType.SECRETARIA,
            professorId = null
        )

        given(autenticarUsuarioUseCase.autenticar("ana@studio.com", "senha123"))
            .willReturn(AutenticacaoResponse("token-fake", usuario))

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"ana@studio.com","senha":"senha123"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").value("token-fake"))
            .andExpect(jsonPath("$.usuario.email").value("ana@studio.com"))
            .andExpect(jsonPath("$.usuario.role").value("SECRETARIA"))
    }

    @Test
    fun deve_retornar_401_quando_credenciais_invalidas() {
        given(autenticarUsuarioUseCase.autenticar(any(), any()))
            .willThrow(CredenciaisInvalidasException("Credenciais inválidas"))

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"errado@studio.com","senha":"errada"}""")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.mensagem").value("Credenciais inválidas"))
    }

    @Test
    fun deve_retornar_400_quando_email_mal_formatado() {
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"nao-e-email","senha":"senha123"}""")
        )
            .andExpect(status().isBadRequest)
    }
}