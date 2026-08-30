package com.crative.studio_api.professor.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "professor")
class ProfessorEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "nome", nullable = false, length = 150)
    var nome: String,

    @Column(name = "telefone", length = 20)
    var telefone: String,

    @Column(name = "especialidade", length = 255)
    var especialidade: String?,

    @Column(name = "ativo", nullable = false)
    var ativo: Boolean = true,

    @Column(name = "criada_em", nullable = false, insertable = false, updatable = false)
    var criadaEm: LocalDateTime? = null
)