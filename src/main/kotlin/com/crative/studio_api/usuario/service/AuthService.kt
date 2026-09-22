package com.crative.studio_api.usuario.service

import com.crative.studio_api.shared.security.JwtService
import com.crative.studio_api.usuario.dto.response.ResultadoAutenticacao
import com.crative.studio_api.usuario.exception.CredenciaisInvalidasException
import com.crative.studio_api.usuario.exception.NovaSenhaIgualAAtualException
import com.crative.studio_api.usuario.exception.SenhaAtualIncorretaException
import com.crative.studio_api.usuario.exception.UsuarioNaoEncontradoException
import com.crative.studio_api.usuario.repository.UsuarioRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

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

    /**
     * Troca a senha do **próprio** usuário logado — o id vem do token, nunca do corpo, então não
     * existe forma de trocar a senha de outra pessoa por aqui.
     *
     * Exigir a senha atual é o que impede que um token vazado vire posse permanente da conta.
     * O erro de senha atual incorreta é 400 (e não 401): o usuário *está* autenticado; o que está
     * errado é o payload. Devolver 401 faria o interceptor do front deslogar quem só errou a digitação.
     *
     * Não emite token novo: o JWT atual continua válido até expirar, como em qualquer sessão viva.
     */
    @Transactional
    fun trocarSenha(usuarioId: UUID, senhaAtual: String, novaSenha: String) {
        val usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow { UsuarioNaoEncontradoException("Usuário não encontrado") }

        if (!usuario.ativo) {
            throw CredenciaisInvalidasException(CREDENCIAS_INVALIDAS)
        }

        if (!passwordEncoder.matches(senhaAtual, usuario.senhaHash)) {
            throw SenhaAtualIncorretaException("Senha atual incorreta")
        }

        if (passwordEncoder.matches(novaSenha, usuario.senhaHash)) {
            throw NovaSenhaIgualAAtualException("A nova senha deve ser diferente da atual")
        }

        usuario.senhaHash = passwordEncoder.encode(novaSenha)!!
        usuarioRepository.save(usuario)
    }
}
