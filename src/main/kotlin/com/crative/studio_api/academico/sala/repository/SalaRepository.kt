package com.crative.studio_api.academico.sala.repository

import com.crative.studio_api.academico.sala.entity.SalaEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SalaRepository: JpaRepository<SalaEntity, UUID> {
    fun findByNome(nome: String): SalaEntity?
}