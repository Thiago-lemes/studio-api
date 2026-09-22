package com.crative.studio_api.usuario.service

import com.crative.studio_api.usuario.entity.RoleType
import com.crative.studio_api.usuario.entity.UsuarioEntity
import com.crative.studio_api.usuario.exception.AutoInativacaoNaoPermitidaException
import com.crative.studio_api.usuario.exception.EmailJaExisteNaBaseException
import com.crative.studio_api.usuario.exception.UltimoAdminAtivoException
import com.crative.studio_api.usuario.exception.UsuarioNaoEncontradoException
import com.crative.studio_api.usuario.repository.UsuarioRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

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
        VinculoProfessorValidator.validar(role, professorId)

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


    @Transactional(readOnly = true)
    fun listar(role: RoleType?, ativo: Boolean?): List<UsuarioEntity> {
        val usuarios = when {
            role != null && ativo != null -> repository.findAllByRoleAndAtivo(role, ativo)
            role != null -> repository.findAllByRole(role)
            ativo != null -> repository.findAllByAtivo(ativo)
            else -> repository.findAll()
        }

        return usuarios.sortedBy { it.nome.lowercase() }
    }

    @Transactional
    fun alterarStatus(id: UUID, ativo: Boolean, solicitanteId: UUID?): UsuarioEntity {
        val usuario = buscarPorId(id)

        if (usuario.ativo == ativo) {
            return usuario
        }

        if (!ativo) {
            validarInativacaoPermitida(usuario, solicitanteId)
        }

        usuario.ativo = ativo
        return repository.save(usuario)
    }

    private fun validarInativacaoPermitida(usuario: UsuarioEntity, solicitanteId: UUID?) {
        if (usuario.id == solicitanteId) {
            throw AutoInativacaoNaoPermitidaException(
                "Você não pode inativar o próprio acesso. Peça a outro administrador."
            )
        }

        if (usuario.role == RoleType.ADMIN && repository.countByRoleAndAtivoTrue(RoleType.ADMIN) <= 1) {
            throw UltimoAdminAtivoException(
                "Este é o único administrador ativo. Promova outro usuário a ADMIN antes de inativá-lo."
            )
        }
    }
}
