package com.crative.studio_api.academico.matricula.controller

import com.crative.studio_api.academico.matricula.dto.request.AlterarStatusMatriculaRequest
import com.crative.studio_api.academico.matricula.dto.request.CriarMatriculaRequest
import com.crative.studio_api.academico.matricula.dto.response.MatriculaResponse
import com.crative.studio_api.academico.matricula.mapper.toResponse
import com.crative.studio_api.academico.matricula.service.MatriculaService
import com.crative.studio_api.academico.matricula.types.StatusMatriculaType
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
import java.util.UUID

@Tag(
    name = "Matrículas",
    description = "Vínculo entre aluno e turma, com os dados financeiros do contrato. " +
            "Criar uma matrícula já gera a conta a receber da competência de início."
)
@RestController
@RequestMapping("/matriculas")
class MatriculaController(
    private val matriculaService: MatriculaService
) {

    @Operation(
        summary = "Matricula um aluno em uma turma",
        description = "Valida aluno ativo, turma ativa, vaga disponível e responsável cadastrado quando o " +
                "aluno é menor de 18 anos. Rematricular um aluno cuja matrícula naquela turma está " +
                "CANCELADA reaproveita o registro existente, com os novos valores."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Matrícula criada e primeira cobrança gerada"),
        ApiResponse(
            responseCode = "400",
            description = "Aluno ou turma inativos, turma sem vaga, menor sem responsável, " +
                    "ou dados financeiros inválidos"
        ),
        ApiResponse(responseCode = "404", description = "Aluno ou turma não encontrados"),
        ApiResponse(responseCode = "409", description = "Aluno já tem matrícula ATIVA ou TRANCADA nessa turma")
    )
    @PostMapping
    fun criar(@Valid @RequestBody request: CriarMatriculaRequest): ResponseEntity<MatriculaResponse> {
        val matricula = matriculaService.criar(
            alunoId = request.alunoId,
            turmaId = request.turmaId,
            valorMensalidade = request.valorMensalidade,
            diaVencimento = request.diaVencimento,
            descontoPercentual = request.descontoPercentual,
            dataInicio = request.dataInicio
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(matricula.toResponse())
    }

    @Operation(
        summary = "Lista matrículas",
        description = "Os filtros são exclusivos e avaliados nesta ordem: alunoId, turmaId, status. " +
                "Sem nenhum filtro, devolve as matrículas ATIVA."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Matrículas encontradas"),
        ApiResponse(responseCode = "404", description = "Aluno ou turma do filtro não encontrados")
    )
    @GetMapping
    fun listar(
        @Parameter(description = "Filtra pelas matrículas de um aluno") @RequestParam(required = false) alunoId: UUID?,
        @Parameter(description = "Filtra pelas matrículas de uma turma") @RequestParam(required = false) turmaId: UUID?,
        @Parameter(description = "Filtra por status") @RequestParam(required = false) status: StatusMatriculaType?
    ): ResponseEntity<List<MatriculaResponse>> {
        val matriculas = when {
            alunoId != null -> matriculaService.listarPorAluno(alunoId)
            turmaId != null -> matriculaService.listarPorTurma(turmaId)
            status != null -> matriculaService.listarPorStatus(status)
            else -> matriculaService.listarPorStatus(StatusMatriculaType.ATIVA)
        }
        return ResponseEntity.ok(matriculas.map { it.toResponse() })
    }

    @Operation(summary = "Busca uma matrícula pelo id")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Matrícula encontrada"),
        ApiResponse(responseCode = "404", description = "Matrícula não encontrada")
    )
    @GetMapping("/{id}")
    fun buscarPorId(
        @Parameter(description = "Id da matrícula") @PathVariable id: UUID
    ): ResponseEntity<MatriculaResponse> {
        return ResponseEntity.ok(matriculaService.buscarPorId(id).toResponse())
    }

    @Operation(
        summary = "Tranca, cancela ou reativa a matrícula",
        description = "Transições permitidas: ATIVA ↔ TRANCADA, e ambas para CANCELADA. " +
                "CANCELADA é terminal — voltar a estudar exige criar a matrícula de novo. " +
                "Cancelar grava a data de fim; reativar exige turma ativa e vaga livre."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Status alterado"),
        ApiResponse(
            responseCode = "400",
            description = "Transição inválida, status repetido, turma inativa ou sem vaga para reativar"
        ),
        ApiResponse(responseCode = "404", description = "Matrícula não encontrada")
    )
    @PatchMapping("/{id}/status")
    fun alterarStatus(
        @Parameter(description = "Id da matrícula") @PathVariable id: UUID,
        @Valid @RequestBody request: AlterarStatusMatriculaRequest
    ): ResponseEntity<MatriculaResponse> {
        val matricula = matriculaService.alterarStatus(id, request.status)
        return ResponseEntity.ok(matricula.toResponse())
    }
}
