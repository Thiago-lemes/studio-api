package com.crative.studio_api.financeiro.mapper

import com.crative.studio_api.financeiro.dto.ContaReceberDetalhada
import com.crative.studio_api.financeiro.dto.response.ContaPagarResponse
import com.crative.studio_api.financeiro.dto.response.ContaReceberResponse
import com.crative.studio_api.financeiro.dto.response.PagamentoResponse
import com.crative.studio_api.financeiro.entity.ContaPagarEntity
import com.crative.studio_api.financeiro.entity.PagamentoEntity

fun ContaPagarEntity.toResponse(): ContaPagarResponse =
    ContaPagarResponse(
        id = requireNotNull(id),
        descricao = descricao,
        categoria = categoria,
        valor = valor,
        vencimento = vencimento,
        status = status,
        pagoEm = pagoEm
    )

fun ContaReceberDetalhada.toResponse(): ContaReceberResponse =
    ContaReceberResponse(
        id = requireNotNull(conta.id),
        matriculaId = conta.matriculaId,
        alunoId = alunoId,
        nomeAluno = nomeAluno,
        referencia = conta.referencia,
        valor = conta.valor,
        vencimento = conta.vencimento,
        status = conta.status,
        diasEmAtraso = diasEmAtraso
    )

fun PagamentoEntity.toResponse(): PagamentoResponse =
    PagamentoResponse(
        id = requireNotNull(id),
        contaReceberId = contaReceberId,
        valorPago = valorPago,
        formaPagamento = formaPagamento,
        pagoEm = pagoEm
    )
