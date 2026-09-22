package com.crative.studio_api.academico.sala.controller

import com.crative.studio_api.academico.sala.dto.request.CadastraSalaRequest
import com.crative.studio_api.academico.sala.dto.response.SalaResponse
import com.crative.studio_api.academico.sala.service.SalaService
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
import java.util.*

/**
 * Os paths REST (`GET /salas`, `GET /salas/{id}`, `PUT /salas/{id}`, `DELETE /salas/{id}`) são os
 * oficiais. Os antigos — `/listar`, `/buscar/{id}`, `/atualizar/{id}`, `/delete/{id}` — continuam
 * funcionando porque o front já os consome, mas estão marcados como depreciados no Swagger e
 * saem numa versão futura.
 *
 * Os dois pares delegam ao mesmo service, então não há chance de divergirem de comportamento.
 */
@Tag(
    name = "Salas",
    description = "Espaços físicos onde as turmas acontecem. Use os paths REST; os caminhos " +
            "`/listar`, `/buscar`, `/atualizar` e `/delete` estão depreciados."
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

    @Operation(
        summary = "Lista todas as salas",
        description = "Sem filtro de ativo — a exclusão de sala é física, então tudo que está aqui existe."
    )
    @ApiResponse(responseCode = "200", description = "Lista de salas")
    @GetMapping
    fun listar(): ResponseEntity<List<SalaResponse>> {
        return ResponseEntity.ok(salaService.listarSalas().map { it.toResponse() })
    }

    @Operation(summary = "Busca uma sala pelo id")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Sala encontrada"),
        ApiResponse(responseCode = "404", description = "Sala não encontrada")
    )
    @GetMapping("/{id}")
    fun buscarPorId(
        @Parameter(description = "Id da sala") @PathVariable id: UUID
    ): ResponseEntity<SalaResponse> {
        return ResponseEntity.ok(salaService.buscarPorId(id).toResponse())
    }

    @Operation(
        summary = "Atualiza nome e capacidade da sala",
        description = "PUT, e não PATCH: o payload sempre foi completo (nome e capacidade), " +
                "o verbo antigo é que contrariava isso."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Sala atualizada"),
        ApiResponse(responseCode = "400", description = "Payload inválido"),
        ApiResponse(responseCode = "404", description = "Sala não encontrada"),
        ApiResponse(responseCode = "409", description = "Já existe sala com esse nome")
    )
    @PutMapping("/{id}")
    fun atualizar(
        @Parameter(description = "Id da sala") @PathVariable id: UUID,
        @Valid @RequestBody request: CadastraSalaRequest
    ): ResponseEntity<SalaResponse> {
        return ResponseEntity.ok(salaService.atualizarSala(id, request.nome, request.capacidade).toResponse())
    }

    @Operation(
        summary = "Remove a sala definitivamente",
        description = "Diferente de aluno e professor, aqui a exclusão é física (não é soft delete)."
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Sala removida"),
        ApiResponse(responseCode = "404", description = "Sala não encontrada")
    )
    @DeleteMapping("/{id}")
    fun remover(
        @Parameter(description = "Id da sala") @PathVariable id: UUID
    ): ResponseEntity<Void> {
        salaService.deletarSala(id)
        return ResponseEntity.noContent().build()
    }

    // --- Paths antigos, mantidos só para não quebrar o front que já os consome ---

    @Operation(summary = "[Depreciado] Lista todas as salas", description = "Use `GET /salas`.", deprecated = true)
    @ApiResponse(responseCode = "200", description = "Lista de salas")
    @Deprecated("Use GET /salas")
    @GetMapping("listar")
    fun listarSalas(): ResponseEntity<List<SalaResponse>> = listar()

    @Operation(
        summary = "[Depreciado] Busca uma sala pelo id",
        description = "Use `GET /salas/{id}`. Atenção: sala inexistente agora responde **404**, não 400.",
        deprecated = true
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Sala encontrada"),
        ApiResponse(responseCode = "404", description = "Sala não encontrada")
    )
    @Deprecated("Use GET /salas/{id}")
    @GetMapping("buscar/{id}")
    fun buscarSalaPorId(
        @Parameter(description = "Id da sala") @PathVariable id: UUID
    ): ResponseEntity<SalaResponse> = buscarPorId(id)

    @Operation(
        summary = "[Depreciado] Atualiza nome e capacidade da sala",
        description = "Use `PUT /salas/{id}`. Atenção: sala inexistente agora responde **404**, não 400.",
        deprecated = true
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Sala atualizada"),
        ApiResponse(responseCode = "400", description = "Payload inválido"),
        ApiResponse(responseCode = "404", description = "Sala não encontrada"),
        ApiResponse(responseCode = "409", description = "Já existe sala com esse nome")
    )
    @Deprecated("Use PUT /salas/{id}")
    @PatchMapping("atualizar/{id}")
    fun atualizarSala(
        @Parameter(description = "Id da sala") @PathVariable id: UUID,
        @Valid @RequestBody request: CadastraSalaRequest
    ): ResponseEntity<SalaResponse> = atualizar(id, request)

    @Operation(
        summary = "[Depreciado] Remove a sala definitivamente",
        description = "Use `DELETE /salas/{id}`. Atenção: sala inexistente agora responde **404**, não 400.",
        deprecated = true
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Sala removida"),
        ApiResponse(responseCode = "404", description = "Sala não encontrada")
    )
    @Deprecated("Use DELETE /salas/{id}")
    @DeleteMapping("delete/{id}")
    fun deletarSala(
        @Parameter(description = "Id da sala") @PathVariable id: UUID
    ): ResponseEntity<Void> = remover(id)
}
