package com.crative.studio_api.aluno.controller

import com.crative.studio_api.aluno.dto.request.AtualizarResponsavelRequest
import com.crative.studio_api.aluno.dto.request.CadastrarResponsavelRequest
import com.crative.studio_api.aluno.dto.response.ResponsavelResponse
import com.crative.studio_api.aluno.mapper.toResponse
import com.crative.studio_api.aluno.service.ResponsavelService
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

@Tag(
    name = "Responsáveis",
    description = "Responsáveis por aluno. Aluno menor de 18 anos precisa de ao menos um responsável " +
            "cadastrado antes de ser matriculado."
)
@RestController
@RequestMapping("/responsaveis")
class ResponsavelController(
    private val responsavelService: ResponsavelService
) {

    @Operation(
        summary = "[Depreciado] Cadastra um responsável para o aluno",
        description = "Use `POST /alunos/{alunoId}/responsaveis`. Responsável é subordinado ao aluno, " +
                "e o path aninhado é o padrão do resto da API. Este caminho continua funcionando " +
                "e some numa versão futura.",
        deprecated = true
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Responsável cadastrado"),
        ApiResponse(responseCode = "400", description = "Payload inválido"),
        ApiResponse(responseCode = "404", description = "Aluno não encontrado")
    )
    @Deprecated("Use POST /alunos/{alunoId}/responsaveis")
    @PostMapping("/aluno/{alunoId}")
    fun cadastrar(
        @Parameter(description = "Id do aluno") @PathVariable alunoId: UUID,
        @Valid @RequestBody request: CadastrarResponsavelRequest
    ): ResponseEntity<ResponsavelResponse> {

        val responsavel = responsavelService.cadastrar(
            alunoId = alunoId,
            nome = request.nome,
            telefone = request.telefone,
            cpf = request.cpf,
            parentesco = request.parentesco
        )

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(responsavel.toResponse())
    }

    @Operation(
        summary = "[Depreciado] Lista os responsáveis de um aluno",
        description = "Use `GET /alunos/{alunoId}/responsaveis`.",
        deprecated = true
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Lista de responsáveis do aluno"),
        ApiResponse(responseCode = "404", description = "Aluno não encontrado")
    )
    @Deprecated("Use GET /alunos/{alunoId}/responsaveis")
    @GetMapping("/aluno/{alunoId}")
    fun listarPorAluno(
        @Parameter(description = "Id do aluno") @PathVariable alunoId: UUID
    ): ResponseEntity<List<ResponsavelResponse>> {

        val responsaveis = responsavelService
            .listarPorAluno(alunoId)
            .map { it.toResponse() }

        return ResponseEntity.ok(responsaveis)
    }

    @Operation(summary = "Atualiza os dados de um responsável")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Responsável atualizado"),
        ApiResponse(responseCode = "404", description = "Responsável não encontrado")
    )
    @PutMapping("/{id}")
    fun atualizar(
        @Parameter(description = "Id do responsável") @PathVariable id: UUID,
        @Valid @RequestBody request: AtualizarResponsavelRequest
    ): ResponseEntity<ResponsavelResponse> {

        val responsavel = responsavelService.atualizar(
            id = id,
            nome = request.nome,
            telefone = request.telefone,
            cpf = request.cpf,
            parentesco = request.parentesco
        )

        return ResponseEntity.ok(responsavel.toResponse())
    }

    @Operation(
        summary = "Remove um responsável",
        description = "Exclusão **física** — responsável cadastrado errado é dado sujo, não histórico.\n\n" +
                "Recusa com 400 a remoção do **último** responsável de um aluno menor de idade que " +
                "tenha matrícula ativa: a validação da matrícula só olha o momento da criação, então " +
                "sem essa trava dava para deixar um menor matriculado sem ninguém respondendo por ele."
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Responsável removido"),
        ApiResponse(responseCode = "400", description = "Único responsável de menor com matrícula ativa"),
        ApiResponse(responseCode = "404", description = "Responsável não encontrado")
    )
    @DeleteMapping("/{id}")
    fun remover(
        @Parameter(description = "Id do responsável") @PathVariable id: UUID
    ): ResponseEntity<Void> {
        responsavelService.remover(id)

        return ResponseEntity.noContent().build()
    }
}
