package com.crative.studio_api.financeiro.controller

import com.crative.studio_api.financeiro.dto.request.RegistrarPagamentoRequest
import com.crative.studio_api.financeiro.dto.response.PagamentoResponse
import com.crative.studio_api.financeiro.mapper.toResponse
import com.crative.studio_api.financeiro.service.PagamentoService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(
    name = "Financeiro — Pagamentos",
    description = "Baixa das mensalidades. Acesso restrito a ADMIN e SECRETARIA."
)
@RestController
@RequestMapping("/pagamentos")
class PagamentoController(
    private val pagamentoService: PagamentoService
) {

    @Operation(
        summary = "Registra o pagamento de uma conta a receber",
        description = "Registra o pagamento e quita a conta no mesmo commit. Não há pagamento parcial: " +
                "qualquer valor registrado leva a conta para PAGO."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Pagamento registrado e conta quitada"),
        ApiResponse(responseCode = "400", description = "Valor pago menor ou igual a zero"),
        ApiResponse(responseCode = "404", description = "Conta a receber não encontrada"),
        ApiResponse(responseCode = "409", description = "Conta a receber já está paga")
    )
    @PostMapping
    fun registrar(@Valid @RequestBody request: RegistrarPagamentoRequest): ResponseEntity<PagamentoResponse> {
        val pagamento = pagamentoService.registrar(
            contaReceberId = request.contaReceberId,
            valorPago = request.valorPago,
            formaPagamento = request.formaPagamento
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(pagamento.toResponse())
    }

    @Operation(summary = "Busca um pagamento pelo id")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Pagamento encontrado"),
        ApiResponse(responseCode = "404", description = "Pagamento não encontrado")
    )
    @GetMapping("/{id}")
    fun buscarPorId(
        @Parameter(description = "Id do pagamento") @PathVariable id: UUID
    ): ResponseEntity<PagamentoResponse> {
        return ResponseEntity.ok(pagamentoService.buscarPorId(id).toResponse())
    }

    @Operation(summary = "Lista os pagamentos de uma conta a receber")
    @ApiResponse(responseCode = "200", description = "Pagamentos da conta informada")
    @GetMapping
    fun listarPorContaReceber(
        @Parameter(description = "Id da conta a receber", required = true) @RequestParam contaReceberId: UUID
    ): ResponseEntity<List<PagamentoResponse>> {
        return ResponseEntity.ok(pagamentoService.listarPorContaReceber(contaReceberId).map { it.toResponse() })
    }
}
