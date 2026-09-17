package com.crative.studio_api.academico.matricula

import com.crative.studio_api.academico.matricula.entity.MatriculaEntity
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Valor da mensalidade já com o desconto aplicado, em 2 casas (HALF_UP).
 * Nunca é persistido na matrícula — é calculado aqui e copiado pra `ContaReceber.valor`
 * no momento em que a cobrança é gerada, pra cobrança não mudar de valor retroativamente
 * se a matrícula for renegociada depois.
 */
fun MatriculaEntity.valorEfetivo(): BigDecimal {
    val fatorDesconto = BigDecimal.ONE - descontoPercentual.divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
    return (valorMensalidade * fatorDesconto).setScale(2, RoundingMode.HALF_UP)
}
