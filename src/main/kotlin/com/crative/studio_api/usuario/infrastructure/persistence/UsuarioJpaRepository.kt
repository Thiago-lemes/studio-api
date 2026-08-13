package com.crative.studio_api.usuario.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UsuarioJpaRepository : JpaRepository<UsuarioEntity, UUID> {
    fun findByEmail(email: String): UsuarioEntity?
    fun findByAtivoTrue(): List<UsuarioEntity>
}
