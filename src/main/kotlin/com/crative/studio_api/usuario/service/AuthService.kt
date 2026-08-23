package com.crative.studio_api.usuario.service

import com.crative.studio_api.shared.security.JwtService
import com.crative.studio_api.usuario.dto.response.ResultadoAutenticacao
import com.crative.studio_api.usuario.exception.CredenciaisInvalidasException
import com.crative.studio_api.usuario.repository.UsuarioRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val usuarioRepository: UsuarioRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder
) {
    companion object {
        private const val CREDENCIAS_INVALIDAS = "Credenciais inválidas"
    }

    fun autenticar(email: String, senha: String): ResultadoAutenticacao {
        val usuario = usuarioRepository.findByEmail(email)
            ?: throw CredenciaisInvalidasException(CREDENCIAS_INVALIDAS)

        if (!usuario.ativo) {
            throw CredenciaisInvalidasException(CREDENCIAS_INVALIDAS)
        }

        if (!passwordEncoder.matches(senha, usuario.senhaHash)) {
            throw CredenciaisInvalidasException(CREDENCIAS_INVALIDAS)
        }
        val token = jwtService.gerarToken(usuario.id, usuario.role, usuario.professorId)

        return ResultadoAutenticacao(token, usuario)
    }
}