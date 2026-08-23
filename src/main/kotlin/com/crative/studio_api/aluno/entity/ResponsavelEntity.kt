package com.crative.studio_api.aluno.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "responsavel")
class ResponsavelEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "aluno_id", nullable = false)
    val alunoId: UUID,

    @Column(nullable = false, length = 150)
    var nome: String,

    @Column(nullable = false, length = 20)
    var telefone: String,

    @Column(length = 14)
    var cpf: String? = null,

    @Column(nullable = false, length = 50)
    var parentesco: String
)