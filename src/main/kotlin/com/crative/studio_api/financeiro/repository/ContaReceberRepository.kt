package com.crative.studio_api.financeiro.repository

import com.crative.studio_api.financeiro.entity.ContaReceberEntity
import com.crative.studio_api.financeiro.types.StatusContaType
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface ContaReceberRepository : JpaRepository<ContaReceberEntity, UUID> {
    fun existsByMatriculaIdAndReferencia(matriculaId: UUID, referencia: String): Boolean
    fun findAllByStatus(status: StatusContaType): List<ContaReceberEntity>
    fun findAllByMatriculaId(matriculaId: UUID): List<ContaReceberEntity>
    fun findAllByMatriculaIdAndStatus(matriculaId: UUID, status: StatusContaType): List<ContaReceberEntity>
    fun findAllByVencimentoBetween(inicio: LocalDate, fim: LocalDate): List<ContaReceberEntity>
    fun findAllByStatusAndVencimentoBefore(status: StatusContaType, data: LocalDate): List<ContaReceberEntity>
}
