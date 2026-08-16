package com.crative.studio_api.shared

import com.crative.studio_api.shared.security.JwtService
import com.crative.studio_api.usuario.domain.RoleType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.*

class JwtServiceTest {
    companion object {
        private const val SECRET_TESTE = "uma-chave-secreta-de-teste-com-pelo-menos-32-caracteres"
    }

    private val jwtService = JwtService(secret = SECRET_TESTE, expirationMs = 28800000)


    @Test
    fun deve_gerar_e_validar_token() {
        val usuarioId = UUID.randomUUID()
        val role = RoleType.ADMIN
        val professorId: UUID? = null

        val token = jwtService.gerarToken(usuarioId, role, professorId)
        val claims = jwtService.validarToken(token)

        assertEquals(usuarioId.toString(), claims.subject)
        assertEquals(role.name, claims["papel"])
    }

    @Test
    fun deve_lancar_excecao_ao_validar_token_expirado() {
        val jwtServiceComExpiracaoInstantanea = JwtService(
            secret = SECRET_TESTE,
            expirationMs = -1000
        )

        val token = jwtServiceComExpiracaoInstantanea.gerarToken(
            usuarioId = UUID.randomUUID(),
            role = RoleType.ADMIN,
            professorId = null
        )

        assertThrows(io.jsonwebtoken.ExpiredJwtException::class.java) {
            jwtService.validarToken(token)
        }
    }

    @Test
    fun deve_lancar_excecao_ao_validar_token_com_assinatura_adulterada() {
        val token = jwtService.gerarToken(
            usuarioId = UUID.randomUUID(),
            role = RoleType.PROFESSOR,
            professorId = UUID.randomUUID()
        )

        val tokenAdulterado = token.dropLast(1) + if (token.last() == 'A') 'B' else 'A'

        assertThrows(io.jsonwebtoken.security.SignatureException::class.java) {
            jwtService.validarToken(tokenAdulterado)
        }
    }
}