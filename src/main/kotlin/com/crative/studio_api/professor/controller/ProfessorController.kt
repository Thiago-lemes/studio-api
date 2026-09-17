package com.crative.studio_api.professor.controller

import com.crative.studio_api.professor.dto.request.AlterarStatusProfessorRequest
import com.crative.studio_api.professor.dto.request.AtualizarProfessorRequest
import com.crative.studio_api.professor.dto.request.CadastrarProfessorRequest
import com.crative.studio_api.professor.dto.response.ProfessorResponse
import com.crative.studio_api.professor.mapper.toResponse
import com.crative.studio_api.professor.service.ProfessorService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@Tag(name = "Professores", description = "Cadastro de professores alocáveis em turmas")
@RestController
@RequestMapping("/professores")
class ProfessorController(
    private val professorService: ProfessorService
) {

    @Operation(
        summary = "Cadastra um professor",
        description = "Duplicidade é checada por nome + telefone (por isso telefone é obrigatório). " +
                "A comparação é literal: 'João' e 'joão' hoje não colidem."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Professor cadastrado"),
        ApiResponse(responseCode = "400", description = "Payload inválido ou nome em branco"),
        ApiResponse(responseCode = "409", description = "Já existe professor com esse nome e telefone")
    )
    @PostMapping
    fun cadastrar(@Valid @RequestBody request: CadastrarProfessorRequest): ResponseEntity<ProfessorResponse> {
        val professor = professorService.cadastrar(
            nome = request.nome,
            telefone = request.telefone,
            especialidade = request.especialidade
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(professor.toResponse())
    }

    @Operation(summary = "Lista os professores ativos")
    @ApiResponse(responseCode = "200", description = "Lista de professores ativos")
    @GetMapping
    fun listarAtivos(): ResponseEntity<List<ProfessorResponse>> {
        return ResponseEntity.ok(professorService.listarAtivos().map { it.toResponse() })
    }

    @Operation(summary = "Busca um professor pelo id")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Professor encontrado"),
        ApiResponse(responseCode = "404", description = "Professor não encontrado")
    )
    @GetMapping("/{id}")
    fun buscarPorId(
        @Parameter(description = "Id do professor") @PathVariable id: UUID
    ): ResponseEntity<ProfessorResponse> {
        return ResponseEntity.ok(professorService.buscarPorId(id).toResponse())
    }

    @Operation(summary = "Atualiza os dados do professor")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Professor atualizado"),
        ApiResponse(responseCode = "404", description = "Professor não encontrado")
    )
    @PutMapping("/{id}")
    fun atualizar(
        @Parameter(description = "Id do professor") @PathVariable id: UUID,
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

    @Operation(
        summary = "Inativa o professor (soft delete)",
        description = "Só marca ativo = false. Turmas já criadas com esse professor continuam existindo."
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Professor inativado"),
        ApiResponse(responseCode = "404", description = "Professor não encontrado")
    )
    @DeleteMapping("/{id}")
    fun inativar(
        @Parameter(description = "Id do professor") @PathVariable id: UUID
    ): ResponseEntity<Void> {
        professorService.alterarStatus(id, ativo = false)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Ativa ou inativa o professor")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Status alterado"),
        ApiResponse(responseCode = "404", description = "Professor não encontrado")
    )
    @PatchMapping("/{id}/status")
    fun alterarStatus(
        @Parameter(description = "Id do professor") @PathVariable id: UUID,
        @Valid @RequestBody request: AlterarStatusProfessorRequest
    ): ResponseEntity<ProfessorResponse> {
        val professor = professorService.alterarStatus(id, request.ativo)
        return ResponseEntity.ok(professor.toResponse())
    }
}
