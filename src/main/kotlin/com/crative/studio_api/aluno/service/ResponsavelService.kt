package com.crative.studio_api.aluno.service

import com.crative.studio_api.aluno.entity.ResponsavelEntity
import com.crative.studio_api.aluno.exception.AlunoNaoEncontradoException
import com.crative.studio_api.aluno.exception.ResponsavelNaoEncontradoException
import com.crative.studio_api.aluno.repository.AlunoRepository
import com.crative.studio_api.aluno.repository.ResponsavelRepository
import org.springframework.stereotype.Service
import java.util.*

@Service
class ResponsavelService(
    private val responsavelRepository: ResponsavelRepository,
    private val alunoRepository: AlunoRepository
) {

    fun cadastrar(
        alunoId: UUID,
        nome: String,
        telefone: String,
        cpf: String?,
        parentesco: String
    ): ResponsavelEntity {

        alunoRepository.findById(alunoId)
            .orElseThrow { AlunoNaoEncontradoException("Aluno não encontrado") }

        val responsavel = ResponsavelEntity(
            alunoId = alunoId,
            nome = nome,
            telefone = telefone,
            cpf = cpf,
            parentesco = parentesco
        )

        return responsavelRepository.save(responsavel)
    }

    fun listarPorAluno(alunoId: UUID): List<ResponsavelEntity> {

        alunoRepository.findById(alunoId)
            .orElseThrow { AlunoNaoEncontradoException("Aluno não encontrado") }

        return responsavelRepository.findAllByAlunoId(alunoId)
    }

    fun atualizar(
        id: UUID,
        nome: String,
        telefone: String,
        cpf: String?,
        parentesco: String
    ): ResponsavelEntity {
        val responsavel = responsavelRepository.findById(id)
            .orElseThrow { ResponsavelNaoEncontradoException("Responsável não encontrado") }

        responsavel.nome = nome
        responsavel.telefone = telefone
        responsavel.cpf = cpf
        responsavel.parentesco = parentesco

        return responsavelRepository.save(responsavel)
    }
}