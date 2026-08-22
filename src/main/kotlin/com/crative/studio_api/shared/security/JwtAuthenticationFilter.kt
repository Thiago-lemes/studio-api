package com.crative.studio_api.shared.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    public override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.removePrefix("Bearer ")

        try {
            val claims = jwtService.validarToken(token)

            val usuarioId = claims.subject
            val papel = claims["papel"] as String
            val professorIdRaw = claims["professorId"] as String?
            val professorId = professorIdRaw?.let { UUID.fromString(it) }

            val authorities = listOf(SimpleGrantedAuthority("ROLE_$papel"))

            val authentication = UsernamePasswordAuthenticationToken(
                usuarioId,
                null,
                authorities
            )
            authentication.details = AuthenticatedUserDetails(professorId)

            SecurityContextHolder.getContext().authentication = authentication
        } catch (ex: Exception) {
            SecurityContextHolder.clearContext()
        }

        filterChain.doFilter(request, response)
    }
}