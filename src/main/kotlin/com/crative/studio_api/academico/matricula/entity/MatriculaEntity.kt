package com.crative.studio_api.academico.matricula.entity

import com.crative.studio_api.academico.matricula.types.StatusMatriculaType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "matricula")
class MatriculaEntity(

    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "aluno_id", nullable = false)
    val alunoId: UUID,

    @Column(name = "turma_id", nullable = false)
    val turmaId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: StatusMatriculaType = StatusMatriculaType.ATIVA,

    @Column(name = "valor_mensalidade", nullable = false, precision = 10, scale = 2)
    var valorMensalidade: BigDecimal,

    @Column(name = "dia_vencimento", nullable = false)
    var diaVencimento: Int,

    @Column(name = "desconto_percentual", precision = 5, scale = 2)
    var descontoPercentual: BigDecimal = BigDecimal.ZERO,

    @Column(name = "data_inicio", nullable = false)
    var dataInicio: LocalDate,

    @Column(name = "data_fim")
    var dataFim: LocalDate? = null
)
