package com.crative.studio_api.usuario.infrastructure.persistence

import com.crative.studio_api.usuario.domain.RoleType
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "usuario")
class UsuarioEntity(

    @Id
    @Column(name = "ID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "NOME", nullable = false)
    val nome: String,

    @Column(name = "EMAIL", nullable = false, unique = true)
    val email: String,

    @Column(name = "SENHA", nullable = false)
    val senhaHash: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false)
    val role: RoleType,

    @Column(name = "PROFESSOR_ID")
    val professorId: UUID? = null,

    @Column(name = "ATIVO", nullable = false)
    val ativo: Boolean = true,

    @Column(name = "CRIADO_EM", nullable = false)
    val criadoEm: LocalDateTime = LocalDateTime.now()
)