package com.crative.studio_api.financeiro.controller

import com.crative.studio_api.financeiro.dto.request.QuitarContaPagarRequest
import com.crative.studio_api.financeiro.dto.request.RegistrarContaPagarRequest
import com.crative.studio_api.financeiro.dto.response.ContaPagarResponse
import com.crative.studio_api.financeiro.mapper.toResponse
import com.crative.studio_api.financeiro.service.ContaPagarService
import com.crative.studio_api.financeiro.types.StatusContaType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.UUID

@Tag(
    name = "Financeiro — Contas a pagar",
    description = "Despesas do estúdio (aluguel, salário, material, marketing, outros). " +
            "Acesso restrito a ADMIN e SECRETARIA."
)
@RestController
@RequestMapping("/contas-pagar")
class ContaPagarController(
    private val contaPagarService: ContaPagarService
) {

    @Operation(
        summary = "Registra uma conta a pagar",
        description = "Conta cadastrada com vencimento já passado nasce ATRASADO — o job diário só alcança " +
                "as que vencem depois de cadastradas."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Conta registrada"),
        ApiResponse(responseCode = "400", description = "Descrição em branco ou valor menor ou igual a zero")
    )
    @PostMapping
    fun registrar(@Valid @RequestBody request: RegistrarContaPagarRequest): ResponseEntity<ContaPagarResponse> {
        val conta = contaPagarService.registrar(
            descricao = request.descricao,
            categoria = request.categoria,
            valor = request.valor,
            vencimento = request.vencimento
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(conta.toResponse())
    }

    @Operation(summary = "Lista as contas a pagar", description = "Sem filtro, devolve todas.")
    @ApiResponse(responseCode = "200", description = "Contas a pagar")
    @GetMapping
    fun listar(
        @Parameter(description = "Filtra por status") @RequestParam(required = false) status: StatusContaType?
    ): ResponseEntity<List<ContaPagarResponse>> {
        return ResponseEntity.ok(contaPagarService.listar(status).map { it.toResponse() })
    }

    @Operation(summary = "Busca uma conta a pagar pelo id")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Conta encontrada"),
        ApiResponse(responseCode = "404", description = "Conta a pagar não encontrada")
    )
    @GetMapping("/{id}")
    fun buscarPorId(
        @Parameter(description = "Id da conta a pagar") @PathVariable id: UUID
    ): ResponseEntity<ContaPagarResponse> {
        return ResponseEntity.ok(contaPagarService.buscarPorId(id).toResponse())
    }

    @Operation(
        summary = "Quita a conta a pagar",
        description = "O body é opcional: sem ele, a data de pagamento é o instante da chamada."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Conta quitada"),
        ApiResponse(responseCode = "404", description = "Conta a pagar não encontrada"),
        ApiResponse(responseCode = "409", description = "Conta já está quitada")
    )
    @PatchMapping("/{id}/pagar")
    fun quitar(
        @Parameter(description = "Id da conta a pagar") @PathVariable id: UUID,
        @RequestBody(required = false) request: QuitarContaPagarRequest?
    ): ResponseEntity<ContaPagarResponse> {
        val dataPagamento = request?.dataPagamento ?: LocalDateTime.now()
        return ResponseEntity.ok(contaPagarService.quitar(id, dataPagamento).toResponse())
    }
}
