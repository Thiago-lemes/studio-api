package com.crative.studio_api.aluno.repository

import com.crative.studio_api.aluno.entity.ResponsavelEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ResponsavelRepository : JpaRepository<ResponsavelEntity, UUID> {

    fun findAllByAlunoId(alunoId: UUID): List<ResponsavelEntity>
}