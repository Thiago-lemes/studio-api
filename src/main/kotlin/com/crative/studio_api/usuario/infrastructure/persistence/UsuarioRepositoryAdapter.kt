package com.crative.studio_api.usuario.infrastructure.persistence

import com.crative.studio_api.usuario.domain.UsuarioDomain
import com.crative.studio_api.usuario.domain.UsuarioRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class UsuarioRepositoryAdapter(
    private val jpaRepository: UsuarioJpaRepository
) : UsuarioRepository {

    override fun salvar(usuarioDomain: UsuarioDomain): UsuarioDomain {
        val entity = usuarioDomain.toEntity()
        val salvo = jpaRepository.save(entity)
        return salvo.toDomain()
    }

    override fun buscarPorId(id: UUID): UsuarioDomain? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun buscarPorEmail(email: String): UsuarioDomain? =
        jpaRepository.findByEmail(email)?.toDomain()

    override fun listarAtivos(): List<UsuarioDomain> =
        jpaRepository.findByAtivoTrue().map { it.toDomain() }
}