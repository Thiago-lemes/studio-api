package com.crative.studio_api.professor.repository

import com.crative.studio_api.professor.entity.ProfessorEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProfessorRepository : JpaRepository<ProfessorEntity, UUID> {
    fun findAllByAtivoTrue(): List<ProfessorEntity>
    fun findByNomeAndTelefone(nome: String, telefone: String): ProfessorEntity?

}