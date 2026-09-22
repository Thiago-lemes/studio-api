package com.crative.studio_api.academico.matricula.controller

import com.crative.studio_api.academico.matricula.dto.response.MatriculaResponse
import com.crative.studio_api.academico.matricula.mapper.toResponse
import com.crative.studio_api.academico.matricula.service.MatriculaService
import com.crative.studio_api.academico.matricula.types.StatusMatriculaType
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
 * `GET /alunos/{id}/matriculas`, que o design previa e nunca existiu.
 *
 * É o mesmo service de `GET /matriculas?alunoId=`; o que muda é só a forma do path, alinhada a
 * `/alunos/{id}/responsaveis` e a `/turmas/{id}/alunos`. As duas rotas convivem.
 */
@Tag(
    name = "Matrículas",
    description = "Vínculo entre aluno e turma, com os dados financeiros do contrato. " +
            "Criar uma matrícula já gera a conta a receber da competência de início."
)
@RestController
@RequestMapping("/alunos/{alunoId}/matriculas")
class AlunoMatriculaController(
    private val matriculaService: MatriculaService
) {

    @Operation(
        summary = "Lista as matrículas do aluno",
        description = "Equivale a `GET /matriculas?alunoId={alunoId}`. Sem `status`, traz **todas** as " +
                "matrículas do aluno, inclusive trancadas e canceladas — é a ficha dele, não a " +
                "listagem geral, então aqui o padrão não filtra por ATIVA."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Matrículas do aluno"),
        ApiResponse(responseCode = "404", description = "Aluno não encontrado")
    )
    @GetMapping
    fun listar(
        @Parameter(description = "Id do aluno") @PathVariable alunoId: UUID,
        @Parameter(description = "Filtra por status") @RequestParam(required = false) status: StatusMatriculaType?
    ): ResponseEntity<List<MatriculaResponse>> {
        return ResponseEntity.ok(
            matriculaService.listar(alunoId, turmaId = null, status = status).map { it.toResponse() }
        )
    }
}
