package com.crative.studio_api.usuario.application.usecase

import com.crative.studio_api.usuario.domain.RoleType
import com.crative.studio_api.usuario.domain.UsuarioDomain
import com.crative.studio_api.usuario.domain.UsuarioRepository
import com.crative.studio_api.usuario.domain.exception.EmailJaExisteNaBaseException
import com.crative.studio_api.usuario.domain.exception.ProfessorNaoDeveSerCadastradoNesseFluxoException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class CriarUsuarioUseCase(
    private val repository: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    fun cadastrarUsuario(nome: String, email: String, senha: String, role: RoleType): UsuarioDomain {
        if (role == RoleType.PROFESSOR) {
            throw ProfessorNaoDeveSerCadastradoNesseFluxoException(
                "Não é permitido criar usuário com role PROFESSOR nesse fluxo"
            )
        }
        repository.buscarPorEmail(email)?.let {
            throw EmailJaExisteNaBaseException("Email já cadastrado")
        }
        val senhaEncode = passwordEncoder.encode(senha)

        val usuario = UsuarioDomain.criar(
            nome = nome,
            email = email,
            senhaHash = senhaEncode!!,
            role = role,
            professorId = null
        )
        return repository.salvar(usuario)
    }
}