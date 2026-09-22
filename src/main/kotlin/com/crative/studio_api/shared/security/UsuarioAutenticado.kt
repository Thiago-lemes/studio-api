package com.crative.studio_api.shared.security

import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

/**
 * Lê o id do usuário logado do `SecurityContext`. O [JwtAuthenticationFilter] guarda o `subject`
 * do JWT (o id do usuário) como principal, sempre em String — daí a conversão aqui.
 */
object UsuarioAutenticado {

    fun id(): UUID? {
        val principal = SecurityContextHolder.getContext().authentication?.principal as? String
            ?: return null

        return runCatching { UUID.fromString(principal) }.getOrNull()
    }
}
