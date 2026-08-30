package com.crative.studio_api.academico.sala.service

import com.crative.studio_api.academico.exception.CapacidadeObrigatoriaException
import com.crative.studio_api.academico.exception.NomeSalaObrigatorioException
import com.crative.studio_api.academico.exception.SalaException
import com.crative.studio_api.academico.exception.SalaJaCadastradaException
import com.crative.studio_api.academico.sala.entity.SalaEntity
import com.crative.studio_api.academico.sala.repository.SalaRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SalaService(private val repository: SalaRepository) {
    fun cadastrar(nome: String, capacidade: Int): SalaEntity {
        validaCadastroSala(nome, capacidade)
        val sala = SalaEntity(nome = nome, capacidade = capacidade)
        return repository.save(sala)
    }

    fun buscarPorId(id: UUID): SalaEntity {
        return repository.findById(id).orElseThrow {
            throw SalaException("Sala não encontrada com o id: $id")
        }
    }

    fun listarSalas(): List<SalaEntity> {
        return repository.findAll()
    }

    fun atualizarSala(id: UUID, nome: String, capacidade: Int): SalaEntity {
        val sala = repository.findById(id).orElseThrow {
            throw SalaException("Sala não encontrada com o id: $id")
        }
        validaCadastroSala(nome, capacidade)
        sala.nome = nome
        sala.capacidade = capacidade
        return repository.save(sala)
    }

    fun deletarSala(id: UUID) {
        val sala = repository.findById(id).orElseThrow {
            throw SalaException("Sala não encontrada com o id: $id")
        }
        repository.delete(sala)
    }

    private fun validaCadastroSala(nome: String, capacidade: Int) {
        repository.findByNome(nome)?.let {
            throw SalaJaCadastradaException("Já existe uma sala cadastrada com o nome: $nome")
        }
        if (nome.isBlank()) {
            throw NomeSalaObrigatorioException("Nome da sala não pode ser nulo ou vazio")
        }
        if (capacidade <= 0) {
            throw CapacidadeObrigatoriaException("Capacidade da sala deve ser maior que zero")
        }
    }
}