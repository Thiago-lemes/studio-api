package com.crative.studio_api.shared.security

import com.crative.studio_api.usuario.entity.RoleType
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.util.*
import javax.crypto.SecretKey

class JwtService(
    secret: String,
    private val expirationMs: Long
) {
    private val signingKey: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun gerarToken(usuarioId: UUID, role: RoleType, professorId: UUID?): String {
        val agora = Date()
        val expiracao = Date(agora.time + expirationMs)

        val builder = Jwts.builder()
            .subject(usuarioId.toString())
            .claim("papel", role.name)
            .issuedAt(agora)
            .expiration(expiracao)
            .signWith(signingKey)

        if (professorId != null) {
            builder.claim("professorId", professorId.toString())
        }
        return builder.compact()
    }

    fun validarToken(token: String): Claims {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}