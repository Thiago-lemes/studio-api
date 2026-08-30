package com.crative.studio_api.professor.controller

import com.crative.studio_api.professor.dto.request.AlterarStatusProfessorRequest
import com.crative.studio_api.professor.dto.request.AtualizarProfessorRequest
import com.crative.studio_api.professor.dto.request.CadastrarProfessorRequest
import com.crative.studio_api.professor.dto.response.ProfessorResponse
import com.crative.studio_api.professor.mapper.toResponse
import com.crative.studio_api.professor.service.ProfessorService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/professores")
class ProfessorController(
    private val professorService: ProfessorService
) {

    @PostMapping
    fun cadastrar(@Valid @RequestBody request: CadastrarProfessorRequest): ResponseEntity<ProfessorResponse> {
        val professor = professorService.cadastrar(
            nome = request.nome,
            telefone = request.telefone,
            especialidade = request.especialidade
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(professor.toResponse())
    }

    @GetMapping
    fun listarAtivos(): ResponseEntity<List<ProfessorResponse>> {
        return ResponseEntity.ok(professorService.listarAtivos().map { it.toResponse() })
    }

    @GetMapping("/{id}")
    fun buscarPorId(@PathVariable id: UUID): ResponseEntity<ProfessorResponse> {
        return ResponseEntity.ok(professorService.buscarPorId(id).toResponse())
    }

    @PutMapping("/{id}")
    fun atualizar(
        @PathVariable id: UUID,
        @Valid @RequestBody request: AtualizarProfessorRequest
    ): ResponseEntity<ProfessorResponse> {
        val professor = professorService.atualizar(
            id = id,
            nome = request.nome,
            telefone = request.telefone,
            especialidade = request.especialidade
        )
        return ResponseEntity.ok(professor.toResponse())
    }

    @DeleteMapping("/{id}")
    fun inativar(@PathVariable id: UUID): ResponseEntity<Void> {
        professorService.alterarStatus(id, ativo = false)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{id}/status")
    fun alterarStatus(
        @PathVariable id: UUID,
        @Valid @RequestBody request: AlterarStatusProfessorRequest
    ): ResponseEntity<ProfessorResponse> {
        val professor = professorService.alterarStatus(id, request.ativo)
        return ResponseEntity.ok(professor.toResponse())
    }
}