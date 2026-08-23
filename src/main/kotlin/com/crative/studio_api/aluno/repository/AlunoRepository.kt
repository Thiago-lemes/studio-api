package com.crative.studio_api.aluno.repository

import com.crative.studio_api.aluno.entity.AlunoEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface AlunoRepository : JpaRepository<AlunoEntity, UUID> {
    fun findAllByAtivoTrue(): List<AlunoEntity>
    fun findByCpf(cpf: String): AlunoEntity?
}

