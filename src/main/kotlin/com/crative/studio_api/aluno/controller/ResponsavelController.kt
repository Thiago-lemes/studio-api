package com.crative.studio_api.aluno.controller

import com.crative.studio_api.aluno.dto.request.AtualizarResponsavelRequest
import com.crative.studio_api.aluno.dto.request.CadastrarResponsavelRequest
import com.crative.studio_api.aluno.dto.response.ResponsavelResponse
import com.crative.studio_api.aluno.mapper.toResponse
import com.crative.studio_api.aluno.service.ResponsavelService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/responsaveis")
class ResponsavelController(
    private val responsavelService: ResponsavelService
) {

    @PostMapping("/aluno/{alunoId}")
    fun cadastrar(
        @PathVariable alunoId: UUID,
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

    @GetMapping("/aluno/{alunoId}")
    fun listarPorAluno(
        @PathVariable alunoId: UUID
    ): ResponseEntity<List<ResponsavelResponse>> {

        val responsaveis = responsavelService
            .listarPorAluno(alunoId)
            .map { it.toResponse() }

        return ResponseEntity.ok(responsaveis)
    }

    @PutMapping("/{id}")
    fun atualizar(
        @PathVariable id: UUID,
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
}