package com.crative.studio_api.usuario.service

import com.crative.studio_api.usuario.entity.RoleType
import com.crative.studio_api.usuario.exception.ProfessorIdNaoPermitidoException
import com.crative.studio_api.usuario.exception.ProfessorIdObrigatorioException
import java.util.UUID

/**
 * Regra compartilhada entre o convite e a criação do usuário: os dois gravam em tabelas com o
 * mesmo CHECK (chk_convite_professor / chk_usuario_professor), então a validação precisa ser
 * literalmente a mesma nos dois caminhos.
 */
object VinculoProfessorValidator {

    fun validar(role: RoleType, professorId: UUID?) {
        if (role == RoleType.PROFESSOR && professorId == null) {
            throw ProfessorIdObrigatorioException("professorId é obrigatório quando a role é igual a PROFESSOR")
        }
        if (role != RoleType.PROFESSOR && professorId != null) {
            throw ProfessorIdNaoPermitidoException("professorId só pode ser preenchido quando role for igual PROFESSOR")
        }
    }
}
