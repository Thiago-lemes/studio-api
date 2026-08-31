package com.crative.studio_api.academico.matricula.repository

import com.crative.studio_api.academico.matricula.entity.MatriculaEntity
import com.crative.studio_api.academico.matricula.types.StatusMatriculaType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MatriculaRepository : JpaRepository<MatriculaEntity, UUID> {
    fun findAllByTurmaIdAndStatus(turmaId: UUID, status: StatusMatriculaType): List<MatriculaEntity>
    fun countByTurmaIdAndStatus(turmaId: UUID, status: StatusMatriculaType): Int
}
