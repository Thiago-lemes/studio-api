package com.crative.studio_api.academico.sala.controller

import com.crative.studio_api.academico.sala.service.SalaService
import com.crative.studio_api.academico.sala.dto.request.CadastraSalaRequest
import com.crative.studio_api.academico.sala.dto.response.SalaResponse
import com.crative.studio_api.academico.sala.toResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@Tag(
    name = "Salas",
    description = "Espaços físicos onde as turmas acontecem. Os paths deste módulo destoam do resto " +
            "da API (usam /listar, /buscar, /atualizar, /delete em vez do path base REST)."
)
@RestController
@RequestMapping("/salas")

class SalaController(
    private val salaService: SalaService
) {
    @Operation(
        summary = "Cadastra uma sala",
        description = "Nome é único e capacidade precisa ser maior que zero."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Sala cadastrada"),
        ApiResponse(responseCode = "400", description = "Nome em branco ou capacidade menor ou igual a zero"),
        ApiResponse(responseCode = "409", description = "Já existe sala com esse nome")
    )
    @PostMapping
    fun cadastrar(@Valid @RequestBody request: CadastraSalaRequest): ResponseEntity<SalaResponse> {
        val sala = salaService.cadastrar(request.nome, request.capacidade).toResponse()
        return ResponseEntity.status(HttpStatus.CREATED).body(sala)
    }

    @Operation(summary = "Lista todas as salas")
    @ApiResponse(responseCode = "200", description = "Lista de salas")
    @GetMapping("listar")
    fun listarSalas(): ResponseEntity<List<SalaResponse>> {
        return ResponseEntity.ok(salaService.listarSalas().map { it.toResponse() })
    }

    @Operation(summary = "Busca uma sala pelo id")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Sala encontrada"),
        ApiResponse(responseCode = "400", description = "Sala não encontrada com o id informado")
    )
    @GetMapping("buscar/{id}")
    fun buscarSalaPorId(
        @Parameter(description = "Id da sala") @PathVariable id: UUID
    ): ResponseEntity<SalaResponse> {
        val sala = salaService.buscarPorId(id).toResponse()
        return ResponseEntity.ok(sala)
    }

    @Operation(
        summary = "Atualiza nome e capacidade da sala",
        description = "Apesar do verbo PATCH, exige o payload completo (nome e capacidade)."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Sala atualizada"),
        ApiResponse(responseCode = "400", description = "Sala não encontrada ou payload inválido"),
        ApiResponse(responseCode = "409", description = "Já existe sala com esse nome")
    )
    @PatchMapping("atualizar/{id}")
    fun atualizarSala(
        @Parameter(description = "Id da sala") @PathVariable id: UUID,
        @Valid @RequestBody request: CadastraSalaRequest
    ): ResponseEntity<SalaResponse> {
        val sala = salaService.atualizarSala(id, request.nome, request.capacidade).toResponse()
        return ResponseEntity.ok(sala)
    }

    @Operation(
        summary = "Remove a sala definitivamente",
        description = "Diferente de aluno e professor, aqui a exclusão é física (não é soft delete)."
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Sala removida"),
        ApiResponse(responseCode = "400", description = "Sala não encontrada com o id informado")
    )
    @DeleteMapping("delete/{id}")
    fun deletarSala(
        @Parameter(description = "Id da sala") @PathVariable id: UUID
    ): ResponseEntity<Void> {
        salaService.deletarSala(id)
        return ResponseEntity.noContent().build()
    }
}
