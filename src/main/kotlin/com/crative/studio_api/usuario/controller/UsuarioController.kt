package com.crative.studio_api.usuario.controller

import com.crative.studio_api.shared.security.UsuarioAutenticado
import com.crative.studio_api.usuario.dto.request.AlterarStatusUsuarioRequest
import com.crative.studio_api.usuario.dto.response.UsuarioResponse
import com.crative.studio_api.usuario.entity.RoleType
import com.crative.studio_api.usuario.mapper.toUsuarioResponse
import com.crative.studio_api.usuario.service.UsuarioService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@Tag(name = "Usuários", description = "Contas de acesso ao sistema (ADMIN, SECRETARIA, PROFESSOR)")
@RestController
@RequestMapping("/usuarios")
class UsuarioController(
    private val service: UsuarioService
) {

    // A criação direta de usuário (POST /usuarios, com senha escolhida pelo ADMIN) foi substituída
    // pelo fluxo de convite — ver ConviteUsuarioController.

    @Operation(
        summary = "Lista os usuários (apenas ADMIN)",
        description = "Ao contrário de `/alunos` e `/professores`, **inclui os inativos por padrão** — a " +
                "tela de gestão de acesso precisa deles para reativar quem foi desligado. Use " +
                "`?ativo=true` para ver só quem tem acesso hoje. Ordenado por nome."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Usuários encontrados"),
        ApiResponse(responseCode = "401", description = "Sem token"),
        ApiResponse(responseCode = "403", description = "Apenas ADMIN")
    )
    @GetMapping
    fun listar(
        @Parameter(description = "Filtra por papel") @RequestParam(required = false) role: RoleType?,
        @Parameter(description = "Filtra por status de acesso. Sem ele, traz ativos e inativos.")
        @RequestParam(required = false) ativo: Boolean?
    ): ResponseEntity<List<UsuarioResponse>> {
        return ResponseEntity.ok(service.listar(role, ativo).map { it.toUsuarioResponse() })
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

    @Operation(
        summary = "Ativa ou inativa o acesso do usuário (apenas ADMIN)",
        description = "É o desligamento de quem saiu do estúdio: `ativo = false` faz o login passar a " +
                "responder 401, sem apagar o histórico da pessoa. Reversível com `ativo = true`.\n\n" +
                "É idempotente — repetir o status atual devolve 200 sem alterar nada.\n\n" +
                "Duas inativações são recusadas com 400: a do próprio usuário logado, e a do último " +
                "ADMIN ativo (ninguém mais conseguiria convidar ou reativar depois)."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Status alterado"),
        ApiResponse(responseCode = "400", description = "Auto-inativação ou último ADMIN ativo"),
        ApiResponse(responseCode = "401", description = "Sem token"),
        ApiResponse(responseCode = "403", description = "Apenas ADMIN"),
        ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    )
    @PatchMapping("/{id}/status")
    fun alterarStatus(
        @Parameter(description = "Id do usuário") @PathVariable id: UUID,
        @Valid @RequestBody request: AlterarStatusUsuarioRequest
    ): ResponseEntity<UsuarioResponse> {
        val usuario = service.alterarStatus(id, request.ativo, UsuarioAutenticado.id())

        return ResponseEntity.ok(usuario.toUsuarioResponse())
    }
}
