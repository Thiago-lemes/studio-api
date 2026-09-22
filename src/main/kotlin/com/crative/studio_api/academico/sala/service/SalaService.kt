package com.crative.studio_api.academico.sala.service

import com.crative.studio_api.academico.exception.CapacidadeObrigatoriaException
import com.crative.studio_api.academico.exception.NomeSalaObrigatorioException
import com.crative.studio_api.academico.exception.SalaJaCadastradaException
import com.crative.studio_api.academico.exception.SalaNaoEncontradaException
import com.crative.studio_api.academico.sala.entity.SalaEntity
import com.crative.studio_api.academico.sala.repository.SalaRepository
import org.springframework.stereotype.Service
import java.util.*

@Service
class SalaService(private val repository: SalaRepository) {
    fun cadastrar(nome: String, capacidade: Int): SalaEntity {
        validaDadosDaSala(nome, capacidade)
        validaNomeDisponivel(nome)
        val sala = SalaEntity(nome = nome, capacidade = capacidade)
        return repository.save(sala)
    }

    fun buscarPorId(id: UUID): SalaEntity {
        return repository.findById(id).orElseThrow {
            SalaNaoEncontradaException("Sala não encontrada com o id: $id")
        }
    }

    fun listarSalas(): List<SalaEntity> {
        return repository.findAll()
    }

    fun atualizarSala(id: UUID, nome: String, capacidade: Int): SalaEntity {
        val sala = buscarPorId(id)
        validaDadosDaSala(nome, capacidade)
        validaNomeDisponivel(nome, ignorarSalaId = id)
        sala.nome = nome
        sala.capacidade = capacidade
        return repository.save(sala)
    }

    fun deletarSala(id: UUID) {
        repository.delete(buscarPorId(id))
    }

    private fun validaDadosDaSala(nome: String, capacidade: Int) {
        if (nome.isBlank()) {
            throw NomeSalaObrigatorioException("Nome da sala não pode ser nulo ou vazio")
        }
        if (capacidade <= 0) {
            throw CapacidadeObrigatoriaException("Capacidade da sala deve ser maior que zero")
        }
    }

    /**
     * Na atualização, a sala editada não conta como duplicata de si mesma — sem o
     * `ignorarSalaId` não era possível alterar só a capacidade, porque o nome atual já
     * estava na base.
     */
    private fun validaNomeDisponivel(nome: String, ignorarSalaId: UUID? = null) {
        val existente = repository.findByNome(nome) ?: return
        if (existente.id != ignorarSalaId) {
            throw SalaJaCadastradaException("Já existe uma sala cadastrada com o nome: $nome")
        }
    }
}