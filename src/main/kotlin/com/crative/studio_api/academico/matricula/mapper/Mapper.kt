package com.crative.studio_api.academico.matricula.mapper

import com.crative.studio_api.academico.matricula.dto.MatriculaDetalhada
import com.crative.studio_api.academico.matricula.dto.response.MatriculaResponse

fun MatriculaDetalhada.toResponse(): MatriculaResponse =
    MatriculaResponse(
        id = requireNotNull(matricula.id),
        alunoId = matricula.alunoId,
        nomeAluno = nomeAluno,
        turmaId = matricula.turmaId,
        modalidadeTurma = modalidadeTurma,
        status = matricula.status,
        valorMensalidade = matricula.valorMensalidade,
        descontoPercentual = matricula.descontoPercentual,
        valorEfetivo = valorEfetivo,
        diaVencimento = matricula.diaVencimento,
        dataInicio = matricula.dataInicio,
        dataFim = matricula.dataFim
    )
