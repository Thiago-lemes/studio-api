package com.crative.studio_api.academico.sala.controller

import com.crative.studio_api.academico.sala.service.SalaService
import com.crative.studio_api.academico.sala.dto.request.CadastraSalaRequest
import com.crative.studio_api.academico.sala.dto.response.SalaResponse
import com.crative.studio_api.academico.sala.toResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/salas")

class SalaController(
    private val salaService: SalaService
) {
    @PostMapping
    fun cadastrar(@Valid @RequestBody request: CadastraSalaRequest): ResponseEntity<SalaResponse> {
        val sala = salaService.cadastrar(request.nome, request.capacidade).toResponse()
        return ResponseEntity.status(HttpStatus.CREATED).body(sala)
    }

    @GetMapping("listar")
    fun listarSalas(): ResponseEntity<List<SalaResponse>> {
        return ResponseEntity.ok(salaService.listarSalas().map { it.toResponse() })
    }

    @GetMapping("buscar/{id}")
    fun buscarSalaPorId(@PathVariable id: UUID): ResponseEntity<SalaResponse> {
        val sala = salaService.buscarPorId(id).toResponse()
        return ResponseEntity.ok(sala)
    }

    @PatchMapping("atualizar/{id}")
    fun atualizarSala(
        @PathVariable id: UUID,
        @Valid @RequestBody request: CadastraSalaRequest
    ): ResponseEntity<SalaResponse> {
        val sala = salaService.atualizarSala(id, request.nome, request.capacidade).toResponse()
        return ResponseEntity.ok(sala)
    }

    @DeleteMapping("delete/{id}")
    fun deletarSala(@PathVariable id: UUID): ResponseEntity<Void> {
        salaService.deletarSala(id)
        return ResponseEntity.noContent().build()
    }
}