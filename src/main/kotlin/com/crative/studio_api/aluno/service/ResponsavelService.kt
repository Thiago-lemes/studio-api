package com.crative.studio_api.aluno.service

import com.crative.studio_api.academico.matricula.repository.MatriculaRepository
import com.crative.studio_api.academico.matricula.types.StatusMatriculaType
import com.crative.studio_api.aluno.entity.ResponsavelEntity
import com.crative.studio_api.aluno.exception.AlunoNaoEncontradoException
import com.crative.studio_api.aluno.exception.ResponsavelNaoEncontradoException
import com.crative.studio_api.aluno.exception.UltimoResponsavelDeMenorException
import com.crative.studio_api.aluno.repository.AlunoRepository
import com.crative.studio_api.aluno.repository.ResponsavelRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Period
import java.util.*

@Service
class ResponsavelService(
    private val responsavelRepository: ResponsavelRepository,
    private val alunoRepository: AlunoRepository,
    private val matriculaRepository: MatriculaRepository
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

    /**
     * Exclusão física — responsável cadastrado errado é dado sujo, não histórico.
     *
     * A trava é a mesma regra que a matrícula já cobra na entrada: aluno menor de 18 anos precisa
     * de ao menos um responsável. Sem ela, dava para esvaziar a lista por aqui e deixar um menor
     * matriculado sem ninguém respondendo por ele — a validação da matrícula só olha o momento da
     * criação, nunca mais depois.
     */
    @Transactional
    fun remover(id: UUID) {
        val responsavel = responsavelRepository.findById(id)
            .orElseThrow { ResponsavelNaoEncontradoException("Responsável não encontrado") }

        validarRemocaoPermitida(responsavel)

        responsavelRepository.delete(responsavel)
    }

    private fun validarRemocaoPermitida(responsavel: ResponsavelEntity) {
        val ehUltimoResponsavel = responsavelRepository.countByAlunoId(responsavel.alunoId) <= 1
        if (!ehUltimoResponsavel) {
            return
        }

        val aluno = alunoRepository.findById(responsavel.alunoId)
            .orElseThrow { AlunoNaoEncontradoException("Aluno não encontrado") }

        val menorDeIdade = Period.between(aluno.dataNascimento, LocalDate.now()).years < 18
        if (!menorDeIdade) {
            return
        }

        val temMatriculaAtiva = matriculaRepository
            .findAllByAlunoIdAndStatus(responsavel.alunoId, StatusMatriculaType.ATIVA)
            .isNotEmpty()

        if (temMatriculaAtiva) {
            throw UltimoResponsavelDeMenorException(
                "Este é o único responsável de um aluno menor de idade com matrícula ativa. " +
                        "Cadastre outro responsável antes de remover este."
            )
        }
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