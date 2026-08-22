package com.crative.studio_api.shared.security

import com.crative.studio_api.usuario.domain.RoleType
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class JwtAuthenticationFilterTest {

    companion object {
        private const val SECRET_TESTE = "uma-chave-secreta-de-teste-com-pelo-menos-32-caracteres"
    }

    private val jwtService = JwtService(secret = SECRET_TESTE, expirationMs = 28800000)

    @Mock
    lateinit var request: HttpServletRequest

    @Mock
    lateinit var response: HttpServletResponse

    @Mock
    lateinit var filterChain: FilterChain

    private lateinit var filter: JwtAuthenticationFilter

    @BeforeEach
    fun setUp() {
        filter = JwtAuthenticationFilter(jwtService)
        SecurityContextHolder.clearContext()
    }

    @Test
    fun deve_popular_security_context_quando_token_valido() {
        val usuarioId = UUID.randomUUID()
        val token = jwtService.gerarToken(usuarioId, RoleType.ADMIN, null)

        given(request.getHeader("Authorization")).willReturn("Bearer $token")

        filter.doFilterInternal(request, response, filterChain)

        val auth = SecurityContextHolder.getContext().authentication

        assertEquals(usuarioId.toString(), auth!!.principal)
        assertEquals(1, auth.authorities.size)
        assertEquals("ROLE_ADMIN", auth.authorities.first().authority)

        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun nao_deve_autenticar_quando_sem_header_authorization() {
        given(request.getHeader("Authorization")).willReturn(null)

        filter.doFilterInternal(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun nao_deve_autenticar_quando_token_invalido() {
        given(request.getHeader("Authorization")).willReturn("Bearer token-invalido-qualquer")

        filter.doFilterInternal(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(filterChain).doFilter(request, response)
    }
}