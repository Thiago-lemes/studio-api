package com.crative.studio_api.academico.turma.controller


import com.crative.studio_api.academico.exception.TurmaAcessoNegadoException
import com.crative.studio_api.academico.turma.dto.request.AtualizarTurmaRequest
import com.crative.studio_api.academico.turma.dto.request.CriarTurmaRequest
import com.crative.studio_api.academico.turma.dto.response.AlunoMatriculadoResponse
import com.crative.studio_api.academico.turma.dto.response.TurmaResponse
import com.crative.studio_api.academico.turma.mapper.toResponse
import com.crative.studio_api.academico.turma.service.TurmaService
import com.crative.studio_api.shared.security.AuthenticatedUserDetails
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/turmas")
class TurmaController(
    private val turmaService: TurmaService
) {

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

    @GetMapping
    fun listarAtivas(): ResponseEntity<List<TurmaResponse>> {
        return ResponseEntity.ok(turmaService.listarAtivas().map { it.toResponse() })
    }

    @GetMapping("/{id}")
    fun buscarPorId(@PathVariable id: UUID): ResponseEntity<TurmaResponse> {
        return ResponseEntity.ok(turmaService.buscarPorId(id).toResponse())
    }

    @PutMapping("/{id}")
    fun atualizar(
        @PathVariable id: UUID,
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

    @GetMapping("/{id}/alunos")
    fun listarAlunosMatriculados(@PathVariable id: UUID): ResponseEntity<List<AlunoMatriculadoResponse>> {
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