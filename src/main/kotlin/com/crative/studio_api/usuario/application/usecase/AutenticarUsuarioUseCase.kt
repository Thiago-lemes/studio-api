package com.crative.studio_api.usuario.application.usecase

import com.crative.studio_api.shared.security.JwtService
import com.crative.studio_api.usuario.domain.UsuarioRepository
import com.crative.studio_api.usuario.domain.exception.CredenciaisInvalidasException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class AutenticarUsuarioUseCase(
    private val usuarioRepository: UsuarioRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder
) {
    companion object {
        private const val CREDENCIAS_INVALIDAS = "Credenciais inválidas"
    }

    fun autenticar(email: String, senha: String): AutenticacaoResponse {
        val usuario = usuarioRepository.buscarPorEmail(email)
            ?: throw CredenciaisInvalidasException(CREDENCIAS_INVALIDAS)

        if (!usuario.ativo) {
            throw CredenciaisInvalidasException(CREDENCIAS_INVALIDAS)
        }

        if (!passwordEncoder.matches(senha, usuario.senhaHash)) {
            throw CredenciaisInvalidasException(CREDENCIAS_INVALIDAS)
        }

        val token = jwtService.gerarToken(usuario.id, usuario.role, usuario.professorId)

        return AutenticacaoResponse(token, usuario)
    }
}