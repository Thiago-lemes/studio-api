package com.crative.studio_api.usuario.controller

import com.crative.studio_api.usuario.dto.request.CriarUsuarioRequest
import com.crative.studio_api.usuario.dto.response.UsuarioCriadoResponse
import com.crative.studio_api.usuario.dto.response.UsuarioResponse
import com.crative.studio_api.usuario.mapper.toCriarUsuarioResponse
import com.crative.studio_api.usuario.mapper.toUsuarioResponse
import com.crative.studio_api.usuario.service.UsuarioService
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

@Tag(name = "Usuários", description = "Contas de acesso ao sistema (ADMIN, SECRETARIA, PROFESSOR)")
@RestController
@RequestMapping("/usuarios")
class UsuarioController(
    private val service: UsuarioService
) {

    @Operation(
        summary = "Cria um usuário do sistema",
        description = "Aceita apenas ADMIN e SECRETARIA. Usuário com role PROFESSOR não é criado por aqui — " +
                "esse fluxo passa pelo autocadastro com aprovação, ainda não implementado."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Usuário criado"),
        ApiResponse(responseCode = "400", description = "Payload inválido ou role PROFESSOR neste fluxo"),
        ApiResponse(responseCode = "409", description = "E-mail já cadastrado"),
        ApiResponse(responseCode = "403", description = "Apenas ADMIN e SECRETARIA")
    )
    @PostMapping
    fun criar(@Valid @RequestBody request: CriarUsuarioRequest): ResponseEntity<UsuarioCriadoResponse> {
        val usuarioCriado = service.cadastrarUsuario(
            nome = request.nome,
            email = request.email,
            senha = request.senha,
            role = request.role
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioCriado.toCriarUsuarioResponse())
    }

    @Operation(summary = "Busca um usuário pelo id")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Usuário encontrado"),
        ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    )
    @GetMapping("/{id}")
    fun buscarPorId(
        @Parameter(description = "Id do usuário") @PathVariable id: UUID
    ): ResponseEntity<UsuarioResponse> {

        val usuario = service.buscarPorId(id)

        return ResponseEntity.ok(
            usuario.toUsuarioResponse()
        )
    }

}
