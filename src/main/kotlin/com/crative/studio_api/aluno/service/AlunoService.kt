package com.crative.studio_api.aluno.service

import com.crative.studio_api.aluno.dto.AlunoDetalhado
import com.crative.studio_api.aluno.entity.AlunoEntity
import com.crative.studio_api.aluno.exception.AlunoNaoEncontradoException
import com.crative.studio_api.aluno.exception.CpfJaCadastradoException
import com.crative.studio_api.aluno.exception.DataNascimentoFuturaException
import com.crative.studio_api.aluno.repository.AlunoRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Period
import java.util.*

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
        return detalhar(buscarEntidadeOuFalhar(id))
    }

    fun listarAtivos(): List<AlunoDetalhado> {
        return repository.findAllByAtivoTrue().map(::detalhar)
    }

    /**
     * Os dois filtros são **combináveis**, e não exclusivos: `?nome=Ana&responsavel=Silva` significa
     * "aluno chamado Ana cujo responsável é um Silva", não um ou outro. Termos em branco são
     * ignorados, então `?nome=` equivale a não filtrar.
     */
    fun listarAtivos(nome: String?, responsavel: String?): List<AlunoDetalhado> {
        val porNome = nome?.takeIf { it.isNotBlank() }
        val porResponsavel = responsavel?.takeIf { it.isNotBlank() }

        val alunos = when {
            porNome != null && porResponsavel != null ->
                repository.buscarAtivosPorResponsavel(porResponsavel)
                    .filter { it.nome.contains(porNome, ignoreCase = true) }

            porResponsavel != null -> repository.buscarAtivosPorResponsavel(porResponsavel)
            porNome != null -> repository.findAllByAtivoTrueAndNomeContainingIgnoreCase(porNome)
            else -> repository.findAllByAtivoTrue()
        }

        return alunos.sortedBy { it.nome.lowercase() }.map(::detalhar)
    }

    fun atualizar(id: UUID, nome: String, telefone: String?): AlunoDetalhado {
        val aluno = buscarEntidadeOuFalhar(id)
        aluno.nome = nome
        aluno.telefone = telefone
        return detalhar(repository.save(aluno))
    }

    fun alterarStatus(id: UUID, ativo: Boolean): AlunoDetalhado {
        val aluno = buscarEntidadeOuFalhar(id)
        aluno.ativo = ativo
        return detalhar(repository.save(aluno))
    }

    private fun buscarEntidadeOuFalhar(id: UUID): AlunoEntity {
        return repository.findById(id)
            .orElseThrow { AlunoNaoEncontradoException("Aluno não encontrado") }
    }

    private fun detalhar(aluno: AlunoEntity): AlunoDetalhado {
        val idade = calcularIdade(aluno.dataNascimento)
        return AlunoDetalhado(aluno = aluno, idade = idade, menorDeIdade = idade < 18)
    }

    private fun calcularIdade(dataNascimento: LocalDate): Int =
        Period.between(dataNascimento, LocalDate.now()).years

    private fun validarDataNascimento(dataNascimento: LocalDate) {
        if (dataNascimento.isAfter(LocalDate.now())) {
            throw DataNascimentoFuturaException("Data de nascimento não pode ser futura")
        }
    }

    private fun validarCpfNaoCadastrado(cpf: String) {
        if (repository.findByCpf(cpf) != null) {
            throw CpfJaCadastradoException("Já existe um aluno cadastrado com este CPF")
        }
    }
}