package com.crative.studio_api.usuario.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
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

    @CreationTimestamp
    @Column(name = "CRIADO_EM", nullable = false, updatable = false)
    var criadoEm: LocalDateTime? = null
)