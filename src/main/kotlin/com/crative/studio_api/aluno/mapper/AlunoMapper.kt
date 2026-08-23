package com.crative.studio_api.aluno.mapper

import com.crative.studio_api.aluno.dto.AlunoDetalhado
import com.crative.studio_api.aluno.dto.response.AlunoResponse

fun AlunoDetalhado.toResponse(): AlunoResponse =
    AlunoResponse(
        id = requireNotNull(aluno.id),
        nome = aluno.nome,
        telefone = aluno.telefone,
        dataNascimento = aluno.dataNascimento,
        cpf = aluno.cpf,
        idade = idade,
        menorDeIdade = menorDeIdade,
        ativo = aluno.ativo
    )