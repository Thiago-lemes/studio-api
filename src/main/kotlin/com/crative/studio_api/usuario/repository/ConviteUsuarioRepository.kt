package com.crative.studio_api.usuario.repository

import com.crative.studio_api.usuario.entity.ConviteUsuarioEntity
import com.crative.studio_api.usuario.entity.StatusConvite
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ConviteUsuarioRepository : JpaRepository<ConviteUsuarioEntity, UUID> {
    fun findByToken(token: String): ConviteUsuarioEntity?
    fun findByEmailAndStatus(email: String, status: StatusConvite): List<ConviteUsuarioEntity>
    fun findAllByOrderByCriadoEmDesc(): List<ConviteUsuarioEntity>
}
