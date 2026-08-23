package com.crative.studio_api.aluno.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "aluno")
class AlunoEntity(

    @Id
    @GeneratedValue
    var id: UUID? = null,

    @Column(nullable = false, length = 150)
    var nome: String,

    @Column(length = 20)
    var telefone: String?,

    @Column(name = "data_nascimento", nullable = false)
    var dataNascimento: LocalDate,

    @Column(length = 14)
    var cpf: String?,

    @Column(nullable = false)
    var ativo: Boolean = true,

    @Column(
        name = "criado_em",
        nullable = false,
        insertable = false,
        updatable = false
    )
    var criadoEm: LocalDateTime? = null
)