package com.crative.studio_api.aluno.controller

import com.crative.studio_api.aluno.dto.request.AlterarStatusAlunoRequest
import com.crative.studio_api.aluno.dto.request.AtualizarAlunoRequest
import com.crative.studio_api.aluno.dto.request.CadastrarAlunoRequest
import com.crative.studio_api.aluno.dto.response.AlunoResponse
import com.crative.studio_api.aluno.mapper.toResponse
import com.crative.studio_api.aluno.service.AlunoService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/alunos")
class AlunoController(
    private val alunoService: AlunoService
) {
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

    @GetMapping
    fun listarAtivos(): ResponseEntity<List<AlunoResponse>> {
        return ResponseEntity.ok(alunoService.listarAtivos().map { it.toResponse() })
    }

    @GetMapping("/{id}")
    fun buscarPorId(@PathVariable id: UUID): ResponseEntity<AlunoResponse> {
        return ResponseEntity.ok(alunoService.buscarPorId(id).toResponse())
    }

    @PutMapping("/{id}")
    fun atualizar(
        @PathVariable id: UUID,
        @Valid @RequestBody request: AtualizarAlunoRequest
    ): ResponseEntity<AlunoResponse> {
        val aluno = alunoService.atualizar(id = id, nome = request.nome, telefone = request.telefone)
        return ResponseEntity.ok(aluno.toResponse())
    }

    @DeleteMapping("/{id}")
    fun inativar(@PathVariable id: UUID): ResponseEntity<Void> {
        alunoService.alterarStatus(id, ativo = false)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{id}/status")
    fun alterarStatus(
        @PathVariable id: UUID,
        @Valid @RequestBody request: AlterarStatusAlunoRequest
    ): ResponseEntity<AlunoResponse> {
        val aluno = alunoService.alterarStatus(id, request.ativo)
        return ResponseEntity.ok(aluno.toResponse())
    }
}