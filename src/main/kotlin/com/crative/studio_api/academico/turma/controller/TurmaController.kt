package com.crative.studio_api.academico.turma.controller


import com.crative.studio_api.academico.exception.TurmaAcessoNegadoException
import com.crative.studio_api.academico.turma.dto.request.AlterarStatusTurmaRequest
import com.crative.studio_api.academico.turma.dto.request.AtualizarTurmaRequest
import com.crative.studio_api.academico.turma.dto.request.CriarTurmaRequest
import com.crative.studio_api.academico.turma.dto.response.AlunoMatriculadoResponse
import com.crative.studio_api.academico.turma.dto.response.TurmaResponse
import com.crative.studio_api.academico.turma.mapper.toResponse
import com.crative.studio_api.academico.turma.service.TurmaService
import com.crative.studio_api.shared.security.AuthenticatedUserDetails
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.*

@Tag(
    name = "Turmas",
    description = "Turmas com dia/horário, professor e sala. Toda criação ou edição valida choque de " +
            "horário em duas frentes: a sala não pode estar ocupada e o professor não pode estar " +
            "escalado em outra turma no mesmo intervalo."
)
@RestController
@RequestMapping("/turmas")
class TurmaController(
    private val turmaService: TurmaService
) {

    @Operation(
        summary = "Cria uma turma",
        description = "Turmas que se encostam não conflitam: uma terminando às 14h e outra começando " +
                "às 14h são aceitas (a comparação de sobreposição é estrita)."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Turma criada"),
        ApiResponse(responseCode = "400", description = "Payload inválido"),
        ApiResponse(responseCode = "404", description = "Professor ou sala não encontrados"),
        ApiResponse(responseCode = "409", description = "Choque de horário na sala ou do professor")
    )
    @PostMapping
    fun criar(@Valid @RequestBody request: CriarTurmaRequest): ResponseEntity<TurmaResponse> {
        val turma = turmaService.criar(
            modalidade = request.modalidade,
            professorId = request.professorId,
            salaId = request.salaId,
            diasSemana = request.diasSemana,
            horarioInicio = request.horarioInicio,
            horarioFim = request.horarioFim,
            capacidadeMaxima = request.capacidadeMaxima
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(turma.toResponse())
    }

    @Operation(
        summary = "Lista as turmas ativas",
        description = "Cada turma vem enriquecida com nome do professor, nome da sala e vagas disponíveis " +
                "(capacidade máxima menos as matrículas ATIVA)."
    )
    @ApiResponse(responseCode = "200", description = "Lista de turmas ativas")
    @GetMapping
    fun listarAtivas(): ResponseEntity<List<TurmaResponse>> {
        return ResponseEntity.ok(turmaService.listarAtivas().map { it.toResponse() })
    }

    @Operation(summary = "Busca uma turma pelo id")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Turma encontrada"),
        ApiResponse(responseCode = "404", description = "Turma não encontrada")
    )
    @GetMapping("/{id}")
    fun buscarPorId(
        @Parameter(description = "Id da turma") @PathVariable id: UUID
    ): ResponseEntity<TurmaResponse> {
        return ResponseEntity.ok(turmaService.buscarPorId(id).toResponse())
    }

    @Operation(
        summary = "Atualiza a turma",
        description = "Revalida choque de horário ignorando a própria turma, pra ela não conflitar consigo mesma."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Turma atualizada"),
        ApiResponse(responseCode = "404", description = "Turma, professor ou sala não encontrados"),
        ApiResponse(responseCode = "409", description = "Choque de horário na sala ou do professor")
    )
    @PutMapping("/{id}")
    fun atualizar(
        @Parameter(description = "Id da turma") @PathVariable id: UUID,
        @Valid @RequestBody request: AtualizarTurmaRequest
    ): ResponseEntity<TurmaResponse> {
        val turma = turmaService.atualizar(
            id = id,
            professorId = request.professorId,
            salaId = request.salaId,
            diasSemana = request.diasSemana,
            horarioInicio = request.horarioInicio,
            horarioFim = request.horarioFim,
            capacidadeMaxima = request.capacidadeMaxima
        )
        return ResponseEntity.ok(turma.toResponse())
    }

    @Operation(
        summary = "Encerra a turma (soft delete)",
        description = "Só marca `ativa = false`, como em aluno e professor — a turma some de `GET /turmas` " +
                "e da agenda, mas o histórico de matrículas continua íntegro.\n\n" +
                "Recusado com 400 enquanto houver matrícula ATIVA: o job de mensalidades continuaria " +
                "cobrando por uma turma fora da grade. Cancele ou tranque as matrículas antes."
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Turma encerrada"),
        ApiResponse(responseCode = "400", description = "Turma ainda tem matrículas ativas"),
        ApiResponse(responseCode = "404", description = "Turma não encontrada")
    )
    @DeleteMapping("/{id}")
    fun encerrar(
        @Parameter(description = "Id da turma") @PathVariable id: UUID
    ): ResponseEntity<Void> {
        turmaService.alterarStatus(id, ativa = false)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Encerra ou reabre a turma",
        description = "Diferente do DELETE, permite **reabrir** uma turma encerrada (`ativa = true`). " +
                "É idempotente.\n\n" +
                "Reabrir revalida choque de horário e pode responder 409: a sala ou o professor podem " +
                "ter sido ocupados por outra turma enquanto esta estava fora da grade."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Status alterado"),
        ApiResponse(responseCode = "400", description = "Turma ainda tem matrículas ativas"),
        ApiResponse(responseCode = "404", description = "Turma não encontrada"),
        ApiResponse(responseCode = "409", description = "Ao reabrir: choque de horário na sala ou do professor")
    )
    @PatchMapping("/{id}/status")
    fun alterarStatus(
        @Parameter(description = "Id da turma") @PathVariable id: UUID,
        @Valid @RequestBody request: AlterarStatusTurmaRequest
    ): ResponseEntity<TurmaResponse> {
        val turma = turmaService.alterarStatus(id, request.ativa)
        return ResponseEntity.ok(turma.toResponse())
    }

    @Operation(
        summary = "Lista os alunos matriculados na turma",
        description = "Só matrículas ATIVA. Um usuário com perfil PROFESSOR só consegue consultar as " +
                "turmas em que ele próprio é o professor; ADMIN e SECRETARIA consultam qualquer turma."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Alunos com matrícula ativa na turma"),
        ApiResponse(responseCode = "403", description = "Professor tentando ver turma de outro professor"),
        ApiResponse(responseCode = "404", description = "Turma não encontrada")
    )
    @GetMapping("/{id}/alunos")
    fun listarAlunosMatriculados(
        @Parameter(description = "Id da turma") @PathVariable id: UUID
    ): ResponseEntity<List<AlunoMatriculadoResponse>> {
        val turma = turmaService.buscarEntidade(id)
        validarAcessoDoProfessor(turma.professorId)

        return ResponseEntity.ok(turmaService.listarAlunosMatriculados(id))
    }

    private fun validarAcessoDoProfessor(professorIdDaTurma: UUID) {
        val authentication = SecurityContextHolder.getContext().authentication
        val isProfessor = authentication!!.authorities.any { it.authority == "ROLE_PROFESSOR" }

        if (isProfessor) {
            val details = authentication.details as? AuthenticatedUserDetails
            val professorIdLogado = details?.professorId

            if (professorIdLogado != professorIdDaTurma) {
                throw TurmaAcessoNegadoException("Você só pode consultar alunos das suas próprias turmas")
            }
        }
    }
}
