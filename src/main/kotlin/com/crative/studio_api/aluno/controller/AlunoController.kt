package com.crative.studio_api.aluno.controller

import com.crative.studio_api.aluno.dto.request.CadastrarAlunoRequest
import com.crative.studio_api.aluno.dto.response.AlunoResponse
import com.crative.studio_api.aluno.mapper.toResponse
import com.crative.studio_api.aluno.service.AlunoService
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

@RestController
@RequestMapping("/alunos")
class AlunoController (
    private val alunoService: AlunoService
){
    @PostMapping
    fun cadastrar(@Valid @RequestBody request: CadastrarAlunoRequest): ResponseEntity<AlunoResponse> {

        val aluno = alunoService.cadastrar(
            nome = request.nome,
            telefone = request.telefone,
            dataNascimento = request.dataNascimento,
            cpf = request.cpf
        )

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(aluno.toResponse())
    }

    @GetMapping("/{id}")
    fun buscarPorId(
        @PathVariable id: UUID
    ): ResponseEntity<AlunoResponse> {

        return ResponseEntity.ok(
            alunoService
                .buscarPorId(id)
                .toResponse()
        )
    }
}