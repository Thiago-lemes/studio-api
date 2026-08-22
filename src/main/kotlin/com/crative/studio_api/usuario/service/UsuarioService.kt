package com.crative.studio_api.usuario.service

import com.crative.studio_api.usuario.entity.RoleType
import com.crative.studio_api.usuario.entity.UsuarioEntity
import com.crative.studio_api.usuario.exception.EmailJaExisteNaBaseException
import com.crative.studio_api.usuario.exception.ProfessorIdNaoPermitidoException
import com.crative.studio_api.usuario.exception.ProfessorIdObrigatorioException
import com.crative.studio_api.usuario.exception.UsuarioNaoEncontradoException
import com.crative.studio_api.usuario.exception.ProfessorNaoDeveSerCadastradoNesseFluxoException
import com.crative.studio_api.usuario.repository.UsuarioRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UsuarioService(
    private val repository: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun cadastrarUsuario(
        nome: String,
        email: String,
        senha: String,
        role: RoleType,
        professorId: UUID? = null
    ): UsuarioEntity {
        if (role == RoleType.PROFESSOR) {
            throw ProfessorNaoDeveSerCadastradoNesseFluxoException(
                "Não é permitido criar usuário com role PROFESSOR nesse fluxo"
            )
        }

        validarVinculoComProfessor(role, professorId)

        repository.findByEmail(email)?.let {
            throw EmailJaExisteNaBaseException("Email já cadastrado")
        }

        val usuario = UsuarioEntity(
            nome = nome,
            email = email,
            senhaHash = passwordEncoder.encode(senha)!!,
            role = role,
            professorId = professorId
        )

        return repository.save(usuario)
    }

    fun buscarPorId(id: UUID): UsuarioEntity {
        return repository.findById(id)
            .orElseThrow {
                UsuarioNaoEncontradoException("Usuário não encontrado")
            }
    }

    private fun validarVinculoComProfessor(role: RoleType, professorId: UUID?) {
        if (role == RoleType.PROFESSOR && professorId == null) {
            throw ProfessorIdObrigatorioException("professorId é obrigatório quando a role é igual a PROFESSOR")
        }
        if (role != RoleType.PROFESSOR && professorId != null) {
            throw ProfessorIdNaoPermitidoException("professorId só pode ser preenchido quando role for igual PROFESSOR")
        }
    }
}