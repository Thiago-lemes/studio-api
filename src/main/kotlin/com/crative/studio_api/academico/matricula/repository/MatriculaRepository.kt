package com.crative.studio_api.academico.matricula.repository

import com.crative.studio_api.academico.matricula.entity.MatriculaEntity
import com.crative.studio_api.academico.matricula.types.StatusMatriculaType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface MatriculaRepository : JpaRepository<MatriculaEntity, UUID> {
    fun findAllByTurmaIdAndStatus(turmaId: UUID, status: StatusMatriculaType): List<MatriculaEntity>
    fun countByTurmaIdAndStatus(turmaId: UUID, status: StatusMatriculaType): Int
    fun findByAlunoIdAndTurmaId(alunoId: UUID, turmaId: UUID): MatriculaEntity?
    fun findAllByAlunoId(alunoId: UUID): List<MatriculaEntity>
    fun findAllByTurmaId(turmaId: UUID): List<MatriculaEntity>
    fun findAllByStatus(status: StatusMatriculaType): List<MatriculaEntity>
    fun countByStatus(status: StatusMatriculaType): Int
    fun findAllByAlunoIdAndStatus(alunoId: UUID, status: StatusMatriculaType): List<MatriculaEntity>
    fun findAllByTurmaIdIn(turmaIds: Collection<UUID>): List<MatriculaEntity>
}
