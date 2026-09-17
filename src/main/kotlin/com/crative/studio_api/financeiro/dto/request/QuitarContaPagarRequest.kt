package com.crative.studio_api.financeiro.dto.request

import java.time.LocalDateTime

data class QuitarContaPagarRequest(
    val dataPagamento: LocalDateTime = LocalDateTime.now()
)
