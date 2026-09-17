package com.crative.studio_api.financeiro.entity

import com.crative.studio_api.financeiro.types.FormaPagamentoType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "pagamento")
class PagamentoEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "conta_receber_id", nullable = false)
    val contaReceberId: UUID,

    @Column(name = "valor_pago", nullable = false, precision = 10, scale = 2)
    val valorPago: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 20)
    val formaPagamento: FormaPagamentoType,

    @Column(name = "pago_em", nullable = false)
    val pagoEm: LocalDateTime = LocalDateTime.now()
)
