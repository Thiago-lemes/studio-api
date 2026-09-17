package com.crative.studio_api.aluno.controller

import com.crative.studio_api.aluno.dto.request.AlterarStatusAlunoRequest
import com.crative.studio_api.aluno.dto.request.AtualizarAlunoRequest
import com.crative.studio_api.aluno.dto.request.CadastrarAlunoRequest
import com.crative.studio_api.aluno.dto.response.AlunoResponse
import com.crative.studio_api.aluno.mapper.toResponse
import com.crative.studio_api.aluno.service.AlunoService
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

@Tag(name = "Alunos", description = "Cadastro de alunos — idade e menoridade são calculadas, nunca persistidas")
@RestController
@RequestMapping("/alunos")
class AlunoController(
    private val alunoService: AlunoService
) {
    @Operation(
        summary = "Cadastra um aluno",
        description = "CPF é obrigatório e único. Data de nascimento não pode ser futura."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Aluno cadastrado"),
        ApiResponse(responseCode = "400", description = "Payload inválido ou data de nascimento futura"),
        ApiResponse(responseCode = "409", description = "CPF já cadastrado")
    )
    @PostMapping
    fun cadastrar(@Valid @RequestBody request: CadastrarAlunoRequest): ResponseEntity<AlunoResponse> {
        val aluno = alunoService.cadastrar(
            nome = request.nome,
            telefone = request.telefone,
            dataNascimento = request.dataNascimento,
            cpf = request.cpf
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(aluno.toResponse())
    }

    @Operation(
        summary = "Lista os alunos ativos",
        description = "Só os ativos — alunos inativados por DELETE ou PATCH /status não aparecem aqui."
    )
    @ApiResponse(responseCode = "200", description = "Lista de alunos ativos")
    @GetMapping
    fun listarAtivos(): ResponseEntity<List<AlunoResponse>> {
        return ResponseEntity.ok(alunoService.listarAtivos().map { it.toResponse() })
    }

    @Operation(summary = "Busca um aluno pelo id")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Aluno encontrado"),
        ApiResponse(responseCode = "404", description = "Aluno não encontrado")
    )
    @GetMapping("/{id}")
    fun buscarPorId(
        @Parameter(description = "Id do aluno") @PathVariable id: UUID
    ): ResponseEntity<AlunoResponse> {
        return ResponseEntity.ok(alunoService.buscarPorId(id).toResponse())
    }

    @Operation(
        summary = "Atualiza nome e telefone do aluno",
        description = "CPF e data de nascimento não são editáveis por este endpoint."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Aluno atualizado"),
        ApiResponse(responseCode = "404", description = "Aluno não encontrado")
    )
    @PutMapping("/{id}")
    fun atualizar(
        @Parameter(description = "Id do aluno") @PathVariable id: UUID,
        @Valid @RequestBody request: AtualizarAlunoRequest
    ): ResponseEntity<AlunoResponse> {
        val aluno = alunoService.atualizar(id = id, nome = request.nome, telefone = request.telefone)
        return ResponseEntity.ok(aluno.toResponse())
    }

    @Operation(
        summary = "Inativa o aluno (soft delete)",
        description = "Não apaga o registro — só marca ativo = false, preservando histórico de matrículas."
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Aluno inativado"),
        ApiResponse(responseCode = "404", description = "Aluno não encontrado")
    )
    @DeleteMapping("/{id}")
    fun inativar(
        @Parameter(description = "Id do aluno") @PathVariable id: UUID
    ): ResponseEntity<Void> {
        alunoService.alterarStatus(id, ativo = false)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Ativa ou inativa o aluno",
        description = "Diferente do DELETE, permite reativar um aluno (ativo = true)."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Status alterado"),
        ApiResponse(responseCode = "404", description = "Aluno não encontrado")
    )
    @PatchMapping("/{id}/status")
    fun alterarStatus(
        @Parameter(description = "Id do aluno") @PathVariable id: UUID,
        @Valid @RequestBody request: AlterarStatusAlunoRequest
    ): ResponseEntity<AlunoResponse> {
        val aluno = alunoService.alterarStatus(id, request.ativo)
        return ResponseEntity.ok(aluno.toResponse())
    }
}
