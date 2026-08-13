package com.crative.studio_api.usuario.domain

import com.crative.studio_api.usuario.domain.exception.ProfessorIdNaoPermitidoException
import com.crative.studio_api.usuario.domain.exception.ProfessorIdObrigatorioException
import java.time.LocalDateTime
import java.util.*

data class UsuarioDomain(
    val id: UUID = UUID.randomUUID(),
    val nome: String,
    val email: String,
    val senhaHash: String,
    val role: RoleType,
    val createdAt: LocalDateTime,
    val ativo: Boolean,
    val professorId: UUID?,
) {
    companion object {
        fun criar(
            nome: String,
            email: String,
            senhaHash: String,
            role: RoleType,
            professorId: UUID?
        ): UsuarioDomain {
            validarVinculoComProfessor(role, professorId)
            return UsuarioDomain(
                id = UUID.randomUUID(),
                nome = nome,
                email = email,
                senhaHash = senhaHash,
                role = role,
                professorId = professorId,
                ativo = true,
                createdAt = LocalDateTime.now()
            )
        }

        private fun validarVinculoComProfessor(role: RoleType, professorId: UUID?) {
            when {
                role == RoleType.PROFESSOR && professorId == null ->
                    throw ProfessorIdObrigatorioException(
                        "professorId é obrigatório quando a role é igual a PROFESSOR"
                    )

                role != RoleType.PROFESSOR && professorId != null ->
                    throw ProfessorIdNaoPermitidoException(
                        "professorId só pode ser preenchido quando role for igual PROFESSOR"
                    )
            }
        }
    }
}