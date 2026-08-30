package com.crative.studio_api.academico.sala.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "sala")
class SalaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "nome", nullable = false, length = 150)
    var nome: String,

    @Column(name = "capacidade", nullable = false)
    var capacidade: Int,
)