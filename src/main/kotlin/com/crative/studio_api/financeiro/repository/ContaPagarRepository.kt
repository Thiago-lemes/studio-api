package com.crative.studio_api.financeiro.repository

import com.crative.studio_api.financeiro.entity.ContaPagarEntity
import com.crative.studio_api.financeiro.types.StatusContaType
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface ContaPagarRepository : JpaRepository<ContaPagarEntity, UUID> {
    fun findAllByStatus(status: StatusContaType): List<ContaPagarEntity>
    fun findAllByVencimentoBetween(inicio: LocalDate, fim: LocalDate): List<ContaPagarEntity>
    fun findAllByStatusAndVencimentoBefore(status: StatusContaType, data: LocalDate): List<ContaPagarEntity>
}
