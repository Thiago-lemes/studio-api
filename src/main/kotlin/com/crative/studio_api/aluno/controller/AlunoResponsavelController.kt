package com.crative.studio_api.aluno.controller

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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Responsável visto de dentro do aluno — o path que o design previa e que `/responsaveis/aluno/{id}`
 * contrariava.
 *
 * Os dois caminhos chamam o mesmo service e convivem de propósito: o front já consome o antigo, e
 * quebrá-lo para arrumar a forma do path custaria mais do que a divergência. O antigo está marcado
 * como depreciado no Swagger.
 *
 * O que **não** ganha par aqui é `PUT`/`DELETE /responsaveis/{id}`: o id do responsável já é único,
 * e repetir o id do aluno no path só abriria a chance de os dois discordarem.
 */
@Tag(
    name = "Responsáveis",
    description = "Responsáveis por aluno. Aluno menor de 18 anos precisa de ao menos um responsável " +
            "cadastrado antes de ser matriculado."
)
@RestController
@RequestMapping("/alunos/{alunoId}/responsaveis")
class AlunoResponsavelController(
    private val responsavelService: ResponsavelService
) {

    @Operation(
        summary = "Cadastra um responsável para o aluno",
        description = "Substitui `POST /responsaveis/aluno/{alunoId}`, que continua funcionando."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Responsável cadastrado"),
        ApiResponse(responseCode = "400", description = "Payload inválido"),
        ApiResponse(responseCode = "404", description = "Aluno não encontrado")
    )
    @PostMapping
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

        return ResponseEntity.status(HttpStatus.CREATED).body(responsavel.toResponse())
    }

    @Operation(
        summary = "Lista os responsáveis do aluno",
        description = "Substitui `GET /responsaveis/aluno/{alunoId}`, que continua funcionando. " +
                "Para editar ou remover um deles, use `PUT`/`DELETE /responsaveis/{id}`."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Lista de responsáveis do aluno"),
        ApiResponse(responseCode = "404", description = "Aluno não encontrado")
    )
    @GetMapping
    fun listar(
        @Parameter(description = "Id do aluno") @PathVariable alunoId: UUID
    ): ResponseEntity<List<ResponsavelResponse>> {
        return ResponseEntity.ok(responsavelService.listarPorAluno(alunoId).map { it.toResponse() })
    }
}
