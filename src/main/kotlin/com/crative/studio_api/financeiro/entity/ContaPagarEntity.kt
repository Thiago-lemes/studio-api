package com.crative.studio_api.financeiro.entity

import com.crative.studio_api.financeiro.types.CategoriaDespesaType
import com.crative.studio_api.financeiro.types.StatusContaType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "conta_pagar")
class ContaPagarEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "descricao", nullable = false, length = 255)
    var descricao: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 20)
    var categoria: CategoriaDespesaType,

    @Column(name = "valor", nullable = false, precision = 10, scale = 2)
    var valor: BigDecimal,

    @Column(name = "vencimento", nullable = false)
    var vencimento: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: StatusContaType = StatusContaType.PENDENTE,

    @Column(name = "pago_em")
    var pagoEm: LocalDateTime? = null
)
