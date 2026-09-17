package com.crative.studio_api.financeiro.controller

import com.crative.studio_api.financeiro.dto.response.DashboardFinanceiroResponse
import com.crative.studio_api.financeiro.exception.ReferenciaInvalidaException
import com.crative.studio_api.financeiro.service.FinanceiroDashboardService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth
import java.time.format.DateTimeParseException

@Tag(
    name = "Financeiro — Dashboard",
    description = "Números consolidados do mês. Acesso restrito a ADMIN e SECRETARIA."
)
@RestController
@RequestMapping("/financeiro")
class FinanceiroDashboardController(
    private val dashboardService: FinanceiroDashboardService
) {

    /** `?referencia=2026-09` (opcional; sem ela, o mês corrente). */
    @Operation(
        summary = "Consolidado financeiro do mês",
        description = """
            Leitura agregada, sem escrita. Definições dos campos:
            - totalReceberMes: tudo que vence no mês, em qualquer status (o previsto)
            - totalRecebidoMes: pagamentos com data no mês — por caixa, não por competência
              (mensalidade de agosto paga em setembro entra em setembro)
            - totalAtrasado: todas as contas ATRASADO, de qualquer competência, não só do mês
            - quantidadeAlunosInadimplentes: alunos distintos por trás dessas contas atrasadas
            - saldoProjetadoMes: totalReceberMes menos totalPagarMes
        """
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Consolidado do mês"),
        ApiResponse(responseCode = "400", description = "Referência fora do formato AAAA-MM")
    )
    @GetMapping("/dashboard")
    fun dashboard(
        @Parameter(description = "Competência no formato AAAA-MM. Sem ela, usa o mês corrente.", example = "2026-09")
        @RequestParam(required = false) referencia: String?
    ): ResponseEntity<DashboardFinanceiroResponse> {
        val mes = referencia?.let(::parsearReferencia) ?: YearMonth.now()
        return ResponseEntity.ok(dashboardService.montar(mes))
    }

    private fun parsearReferencia(referencia: String): YearMonth {
        return try {
            YearMonth.parse(referencia)
        } catch (ex: DateTimeParseException) {
            throw ReferenciaInvalidaException("Referência inválida: use o formato AAAA-MM (ex: 2026-09)")
        }
    }
}
