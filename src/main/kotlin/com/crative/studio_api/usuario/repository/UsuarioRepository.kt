package com.crative.studio_api.usuario.repository

import com.crative.studio_api.usuario.entity.RoleType
import com.crative.studio_api.usuario.entity.UsuarioEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UsuarioRepository : JpaRepository<UsuarioEntity, UUID> {
    fun findByEmail(email: String): UsuarioEntity?
    fun findByAtivoTrue(): List<UsuarioEntity>
    fun findByEmailAndAtivoTrue(email: String): List<UsuarioEntity>
    fun findByIdAndAtivoTrue(id: UUID): UsuarioEntity?
    fun findByProfessorId(professorId: UUID): UsuarioEntity?

    fun findAllByRole(role: RoleType): List<UsuarioEntity>
    fun findAllByAtivo(ativo: Boolean): List<UsuarioEntity>
    fun findAllByRoleAndAtivo(role: RoleType, ativo: Boolean): List<UsuarioEntity>

    /** Usado para impedir que o último ADMIN ativo seja inativado e tranque a gestão do sistema. */
    fun countByRoleAndAtivoTrue(role: RoleType): Long
}
