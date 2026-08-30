package com.crative.studio_api.professor.service

import com.crative.studio_api.professor.entity.ProfessorEntity
import com.crative.studio_api.professor.exception.NomeObrigatorioException
import com.crative.studio_api.professor.exception.ProfessorJaCadastradoException
import com.crative.studio_api.professor.exception.ProfessorNaoEncontradoException
import com.crative.studio_api.professor.repository.ProfessorRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ProfessorService(
    private val repository: ProfessorRepository
) {

    fun cadastrar(nome: String, telefone: String, especialidade: String?): ProfessorEntity {
        validarNome(nome)
        validarNaoDuplicado(nome, telefone)

        val professor = ProfessorEntity(
            nome = nome,
            telefone = telefone,
            especialidade = especialidade
        )

        return repository.save(professor)
    }

    fun buscarPorId(id: UUID): ProfessorEntity {
        return buscarEntidadeOuFalhar(id)
    }

    fun listarAtivos(): List<ProfessorEntity> {
        return repository.findAllByAtivoTrue()
    }

    fun atualizar(id: UUID, nome: String, telefone: String, especialidade: String?): ProfessorEntity {
        validarNome(nome)
        val professor = buscarEntidadeOuFalhar(id)

        professor.nome = nome
        professor.telefone = telefone
        professor.especialidade = especialidade

        return repository.save(professor)
    }

    fun alterarStatus(id: UUID, ativo: Boolean): ProfessorEntity {
        val professor = buscarEntidadeOuFalhar(id)
        professor.ativo = ativo
        return repository.save(professor)
    }

    private fun buscarEntidadeOuFalhar(id: UUID): ProfessorEntity {
        return repository.findById(id)
            .orElseThrow { ProfessorNaoEncontradoException("Professor não encontrado") }
    }

    private fun validarNome(nome: String) {
        if (nome.isBlank()) {
            throw NomeObrigatorioException("Nome é obrigatório")
        }
    }

    private fun validarNaoDuplicado(nome: String, telefone: String) {
        if (repository.findByNomeAndTelefone(nome, telefone) != null) {
            throw ProfessorJaCadastradoException("Já existe um professor cadastrado com esse nome e telefone")
        }
    }
}