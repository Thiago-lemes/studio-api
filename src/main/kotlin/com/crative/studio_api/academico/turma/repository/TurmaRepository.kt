package com.crative.studio_api.academico.turma.repository

import com.crative.studio_api.academico.turma.entity.TurmaEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface TurmaRepository : JpaRepository<TurmaEntity, UUID> {
    fun findAllBySalaIdAndAtivaTrue(salaId: UUID): List<TurmaEntity>
    fun findAllByAtivaTrue(): List<TurmaEntity>
    fun countByAtivaTrue(): Int
    fun findAllByProfessorIdAndAtivaTrue(professorId: UUID): List<TurmaEntity>
}