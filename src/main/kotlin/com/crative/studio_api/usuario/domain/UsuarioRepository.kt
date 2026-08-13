package com.crative.studio_api.usuario.domain

import java.util.UUID

interface UsuarioRepository {
    fun salvar(usuarioDomain: UsuarioDomain): UsuarioDomain
    fun buscarPorId(id: UUID): UsuarioDomain?
    fun buscarPorEmail(email: String): UsuarioDomain?
    fun listarAtivos(): List<UsuarioDomain>
}