package com.crative.studio_api.professor.repository

import com.crative.studio_api.professor.entity.ProfessorEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ProfessorRepository : JpaRepository<ProfessorEntity, UUID> {
    fun findAllByAtivoTrue(): List<ProfessorEntity>
    fun countByAtivoTrue(): Int
    fun findByNomeAndTelefone(nome: String, telefone: String): ProfessorEntity?

}