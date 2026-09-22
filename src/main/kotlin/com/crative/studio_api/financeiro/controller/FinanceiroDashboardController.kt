package com.crative.studio_api.financeiro.controller

import com.crative.studio_api.financeiro.dto.response.DashboardFinanceiroResponse
import com.crative.studio_api.financeiro.dto.response.RelatorioFinanceiroResponse
import com.crative.studio_api.financeiro.exception.ReferenciaInvalidaException
import com.crative.studio_api.financeiro.service.FinanceiroDashboardService
import com.crative.studio_api.financeiro.service.RelatorioFinanceiroService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

@Tag(
    name = "Financeiro — Dashboard e relatórios",
    description = "Números consolidados do mês e fechamento por período. Acesso restrito a ADMIN e SECRETARIA."
)
@RestController
@RequestMapping("/financeiro")
class FinanceiroDashboardController(
    private val dashboardService: FinanceiroDashboardService,
    private val relatorioService: RelatorioFinanceiroService
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

    @Operation(
        summary = "Relatório financeiro de um período",
        description = """
            Fechamento de um intervalo livre de datas — o que substitui a soma feita no navegador
            e destrava a exportação em PDF. Diferente de `/financeiro/dashboard`, que é preso a uma
            competência AAAA-MM.

            O intervalo é **fechado dos dois lados** (inclui `de` e `ate`).

            Os números vêm em dois regimes, e misturá-los é o erro clássico deste relatório:
            - **realizado** (`totalRecebido`, `totalPago`): dinheiro que entrou ou saiu dentro do
              período, pela data do pagamento — mensalidade de agosto paga em setembro conta em setembro
            - **previsto** (`totalAReceber`, `totalAPagar`): tudo que vence no período, em qualquer status

            Por isso `saldoRealizado` e `saldoPrevisto` andam separados: um responde "quanto sobrou
            no caixa", o outro "quanto deveria sobrar".

            `totalAtrasado` foge da janela de propósito — é o estoque de inadimplência acumulado até
            `ate`, de qualquer competência.
        """
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Relatório do período"),
        ApiResponse(
            responseCode = "400",
            description = "Data final anterior à inicial, ou data fora do formato AAAA-MM-DD"
        )
    )
    @GetMapping("/relatorio")
    fun relatorio(
        @Parameter(description = "Início do período (inclusivo)", required = true, example = "2026-09-01")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) de: LocalDate,

        @Parameter(description = "Fim do período (inclusivo)", required = true, example = "2026-09-30")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) ate: LocalDate
    ): ResponseEntity<RelatorioFinanceiroResponse> {
        return ResponseEntity.ok(relatorioService.montar(de, ate))
    }

    private fun parsearReferencia(referencia: String): YearMonth {
        return try {
            YearMonth.parse(referencia)
        } catch (ex: DateTimeParseException) {
            throw ReferenciaInvalidaException("Referência inválida: use o formato AAAA-MM (ex: 2026-09)")
        }
    }
}
