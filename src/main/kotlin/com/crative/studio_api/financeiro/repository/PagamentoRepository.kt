package com.crative.studio_api.financeiro.repository

import com.crative.studio_api.financeiro.entity.PagamentoEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.UUID

interface PagamentoRepository : JpaRepository<PagamentoEntity, UUID> {
    fun findAllByContaReceberId(contaReceberId: UUID): List<PagamentoEntity>
    fun findAllByPagoEmBetween(inicio: LocalDateTime, fim: LocalDateTime): List<PagamentoEntity>
}
