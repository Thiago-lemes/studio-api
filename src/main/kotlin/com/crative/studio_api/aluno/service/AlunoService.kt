package com.crative.studio_api.aluno.service

import com.crative.studio_api.aluno.dto.AlunoDetalhado
import com.crative.studio_api.aluno.entity.AlunoEntity
import com.crative.studio_api.aluno.exception.AlunoNaoEncontradoException
import com.crative.studio_api.aluno.exception.CpfJaCadastradoException
import com.crative.studio_api.aluno.exception.CpfNaoPodeSerNull
import com.crative.studio_api.aluno.exception.DataNascimentoFuturaException
import com.crative.studio_api.aluno.repository.AlunoRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Period
import java.util.UUID

@Service
class AlunoService(
    private val repository: AlunoRepository
) {
    fun cadastrar(
        nome: String,
        telefone: String?,
        dataNascimento: LocalDate,
        cpf: String
    ): AlunoDetalhado {
        validarDataNascimento(dataNascimento)
        validarCpfNaoCadastrado(cpf)
        val aluno = AlunoEntity(
            nome = nome,
            telefone = telefone,
            dataNascimento = dataNascimento,
            cpf = cpf
        )

        val alunoSalvo = repository.save(aluno)

        return detalhar(alunoSalvo)
    }

    fun buscarPorId(id: UUID): AlunoDetalhado {

        val aluno = repository.findById(id)
            .orElseThrow {
                AlunoNaoEncontradoException(
                    "Aluno não encontrado"
                )
            }

        return detalhar(aluno)
    }

    fun listarAtivos(): List<AlunoDetalhado> {

        return repository.findAllByAtivoTrue()
            .map(::detalhar)
    }

    private fun detalhar(aluno: AlunoEntity): AlunoDetalhado {

        val idade = calcularIdade(
            aluno.dataNascimento
        )

        return AlunoDetalhado(
            aluno = aluno,
            idade = idade,
            menorDeIdade = idade < 18
        )
    }

    private fun calcularIdade(
        dataNascimento: LocalDate
    ): Int {

        return Period.between(
            dataNascimento,
            LocalDate.now()
        ).years
    }

    private fun validarDataNascimento(
        dataNascimento: LocalDate
    ) {

        if (dataNascimento.isAfter(LocalDate.now())) {
            throw DataNascimentoFuturaException(
                "Data de nascimento não pode ser futura"
            )
        }
    }

    private fun validarCpfNaoCadastrado(cpf: String) {
        if (cpf.isBlank()) {
            throw CpfNaoPodeSerNull(
                "CPF é obrigatorio"
            )
        }
        if (repository.findByCpf(cpf) != null) {
            throw CpfJaCadastradoException(
                "Já existe um aluno cadastrado com este CPF"
            )
        }


    }
}
