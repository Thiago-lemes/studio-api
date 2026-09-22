package com.crative.studio_api.usuario.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "convite_usuario")
class ConviteUsuarioEntity(

    @Id
    @Column(name = "ID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "EMAIL", nullable = false)
    val email: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false)
    val role: RoleType,

    @Column(name = "PROFESSOR_ID")
    val professorId: UUID? = null,

    @Column(name = "TOKEN", nullable = false, unique = true)
    val token: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    var status: StatusConvite = StatusConvite.PENDENTE,

    /**
     * Calculado no service a partir de `app.convite.validade-horas` — não como default aqui, para
     * o relógio não ficar preso ao momento da construção da entidade e para o teste conseguir
     * criar um convite já vencido sem manipular o relógio.
     */
    @Column(name = "EXPIRA_EM", nullable = false)
    val expiraEm: LocalDateTime,

    @CreationTimestamp
    @Column(name = "CRIADO_EM", nullable = false, updatable = false)
    var criadoEm: LocalDateTime? = null
)
