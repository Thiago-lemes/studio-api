package com.crative.studio_api.usuario.repository

import com.crative.studio_api.usuario.entity.UsuarioEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UsuarioRepository : JpaRepository<UsuarioEntity, UUID> {
    fun findByEmail(email: String): UsuarioEntity?
    fun findByAtivoTrue(): List<UsuarioEntity>
    fun findByEmailAndAtivoTrue(email: String): List<UsuarioEntity>
    fun findByIdAndAtivoTrue(id: UUID): UsuarioEntity?
}