package com.crative.studio_api.dashboard.controller

import com.crative.studio_api.dashboard.dto.AgendaAulaResponse
import com.crative.studio_api.dashboard.dto.DashboardGeralResponse
import com.crative.studio_api.dashboard.service.AgendaService
import com.crative.studio_api.dashboard.service.DashboardGeralService
import com.crative.studio_api.financeiro.exception.ReferenciaInvalidaException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException
import java.util.UUID

@Tag(
    name = "Dashboard e agenda",
    description = "Leituras agregadas que existiam só no cliente: a visão de abertura do sistema e " +
            "o calendário derivado das turmas."
)
@RestController
class DashboardController(
    private val dashboardGeralService: DashboardGeralService,
    private val agendaService: AgendaService
) {

    @Operation(
        summary = "Visão geral do estúdio",
        description = "Substitui as três listagens completas que o front baixava só para contar linhas — " +
                "o custo crescia com a base inteira para exibir meia dúzia de números.\n\n" +
                "O bloco `financeiro` é exatamente o de `GET /financeiro/dashboard`, com as mesmas " +
                "definições de recebido, previsto e atrasado.\n\n" +
                "`vagasDisponiveis` é a soma das vagas livres **por turma**, não " +
                "`capacidadeTotal - matriculasAtivas`: turma lotada não empresta vaga para turma vazia."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Visão geral"),
        ApiResponse(responseCode = "400", description = "Referência fora do formato AAAA-MM"),
        ApiResponse(responseCode = "401", description = "Sem token")
    )
    @GetMapping("/dashboard")
    fun geral(
        @Parameter(description = "Competência do bloco financeiro (AAAA-MM). Sem ela, o mês corrente.", example = "2026-09")
        @RequestParam(required = false) referencia: String?
    ): ResponseEntity<DashboardGeralResponse> {
        val mes = referencia?.let(::parsearReferencia) ?: YearMonth.now()

        return ResponseEntity.ok(dashboardGeralService.montar(mes))
    }

    @Operation(
        summary = "Agenda de aulas do período",
        description = "Expande a recorrência semanal das turmas ativas em ocorrências datadas — o que o " +
                "calendário derivava de `GET /turmas` no cliente. Intervalo fechado dos dois lados.\n\n" +
                "**Não há entidade de aula por trás disto.** A agenda é pura derivação da grade, então " +
                "não existe aula cancelada, feriado nem reposição: toda semana do intervalo produz as " +
                "mesmas ocorrências. Marcar exceções exige uma tabela de aula, que ainda não existe.\n\n" +
                "O período é limitado a 366 dias, para um intervalo largo não gerar milhões de linhas."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Ocorrências ordenadas por data e horário"),
        ApiResponse(responseCode = "400", description = "Data final anterior à inicial, ou período acima de 366 dias"),
        ApiResponse(responseCode = "401", description = "Sem token")
    )
    @GetMapping("/agenda")
    fun agenda(
        @Parameter(description = "Início do período (inclusivo)", required = true, example = "2026-09-01")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) de: LocalDate,

        @Parameter(description = "Fim do período (inclusivo)", required = true, example = "2026-09-30")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) ate: LocalDate,

        @Parameter(description = "Filtra pelas turmas de um professor")
        @RequestParam(required = false) professorId: UUID?,

        @Parameter(description = "Filtra pelas turmas de uma sala")
        @RequestParam(required = false) salaId: UUID?
    ): ResponseEntity<List<AgendaAulaResponse>> {
        return ResponseEntity.ok(agendaService.listar(de, ate, professorId, salaId))
    }

    private fun parsearReferencia(referencia: String): YearMonth {
        return try {
            YearMonth.parse(referencia)
        } catch (ex: DateTimeParseException) {
            throw ReferenciaInvalidaException("Referência inválida: use o formato AAAA-MM (ex: 2026-09)")
        }
    }
}
