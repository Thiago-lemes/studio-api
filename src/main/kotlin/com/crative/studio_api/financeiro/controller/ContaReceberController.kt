package com.crative.studio_api.financeiro.controller

import com.crative.studio_api.financeiro.dto.response.ContaReceberResponse
import com.crative.studio_api.financeiro.mapper.toResponse
import com.crative.studio_api.financeiro.service.ContaReceberService
import com.crative.studio_api.financeiro.types.StatusContaType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Sem POST: conta a receber só nasce via `MatriculaService.criar` ou pelo `GerarMensalidadesJob`.
 */
@Tag(
    name = "Financeiro — Contas a receber",
    description = "Mensalidades dos alunos. Somente leitura: não existe POST — a cobrança nasce junto " +
            "com a matrícula ou pelo job mensal que roda todo dia 1. Acesso restrito a ADMIN e SECRETARIA."
)
@RestController
@RequestMapping("/contas-receber")
class ContaReceberController(
    private val contaReceberService: ContaReceberService
) {

    @Operation(
        summary = "Lista as contas a receber",
        description = "Filtros combináveis. Cada item traz o aluno por trás da matrícula e, quando a conta " +
                "está ATRASADO, há quantos dias ela venceu."
    )
    @ApiResponse(responseCode = "200", description = "Contas a receber")
    @GetMapping
    fun listar(
        @Parameter(description = "Filtra por status") @RequestParam(required = false) status: StatusContaType?,
        @Parameter(description = "Filtra pelas cobranças de uma matrícula")
        @RequestParam(required = false) matriculaId: UUID?
    ): ResponseEntity<List<ContaReceberResponse>> {
        return ResponseEntity.ok(contaReceberService.listar(status, matriculaId).map { it.toResponse() })
    }

    @Operation(summary = "Busca uma conta a receber pelo id")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Conta encontrada"),
        ApiResponse(responseCode = "404", description = "Conta a receber não encontrada")
    )
    @GetMapping("/{id}")
    fun buscarPorId(
        @Parameter(description = "Id da conta a receber") @PathVariable id: UUID
    ): ResponseEntity<ContaReceberResponse> {
        return ResponseEntity.ok(contaReceberService.buscarPorId(id).toResponse())
    }
}
