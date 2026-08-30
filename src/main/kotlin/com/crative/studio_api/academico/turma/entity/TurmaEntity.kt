package com.crative.studio_api.academico.turma.entity

import com.crative.studio_api.academico.turma.types.DiaSemanaType
import jakarta.persistence.*
import java.time.LocalTime
import java.util.*

@Entity
@Table(name = "turma")
class TurmaEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "modalidade", nullable = false, length = 100)
    var modalidade: String,

    @Column(name = "professor_id", nullable = false)
    var professorId: UUID,

    @Column(name = "sala_id", nullable = false)
    var salaId: UUID,

    @ElementCollection(targetClass = DiaSemanaType::class, fetch = FetchType.EAGER)
    @CollectionTable(name = "turma_dia_semana", joinColumns = [JoinColumn(name = "turma_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false)
    var diasSemana: MutableSet<DiaSemanaType>,

    @Column(name = "horario_inicio", nullable = false)
    var horarioInicio: LocalTime,

    @Column(name = "horario_fim", nullable = false)
    var horarioFim: LocalTime,

    @Column(name = "capacidade_maxima", nullable = false)
    var capacidadeMaxima: Int,

    @Column(name = "ativa", nullable = false)
    var ativa: Boolean = true
)