package com.crative.studio_api.usuario.controller

import com.crative.studio_api.usuario.dto.request.CompletarCadastroRequest
import com.crative.studio_api.usuario.dto.request.CriarConviteRequest
import com.crative.studio_api.usuario.dto.response.ConviteCriadoResponse
import com.crative.studio_api.usuario.dto.response.ConviteResumoResponse
import com.crative.studio_api.usuario.dto.response.ConviteValidoResponse
import com.crative.studio_api.usuario.entity.StatusConvite
import com.crative.studio_api.usuario.mapper.toConviteCriadoResponse
import com.crative.studio_api.usuario.mapper.toConviteResumoResponse
import com.crative.studio_api.usuario.mapper.toConviteValidoResponse
import com.crative.studio_api.usuario.service.ConviteUsuarioService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(
    name = "Convites de usuário",
    description = "ADMIN convida por e-mail; a pessoa abre o link e define nome e senha"
)
@RestController
@RequestMapping("/usuarios/convites")
class ConviteUsuarioController(
    private val service: ConviteUsuarioService
) {

    @Operation(
        summary = "Cria um convite de acesso (apenas ADMIN)",
        description = "O ADMIN informa apenas e-mail e papel. Se o papel for PROFESSOR, a ficha do " +
                "professor é criada no momento em que a própria pessoa completa o cadastro.\n\n" +
                "Enquanto o envio automático de e-mail não existe, a resposta devolve `linkConvite` " +
                "e `token` para o ADMIN repassar manualmente. Os dois campos saem da resposta quando " +
                "o notification-service entrar — não construa tela que dependa deles em regime permanente."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Convite criado"),
        ApiResponse(responseCode = "400", description = "Payload inválido"),
        ApiResponse(responseCode = "401", description = "Sem token"),
        ApiResponse(responseCode = "403", description = "Apenas ADMIN"),
        ApiResponse(responseCode = "409", description = "E-mail já possui usuário")
    )
    @PostMapping
    fun criar(@Valid @RequestBody request: CriarConviteRequest): ResponseEntity<ConviteCriadoResponse> {
        val criado = service.criar(request.email, request.role)

        return ResponseEntity.status(HttpStatus.CREATED).body(criado.toConviteCriadoResponse())
    }

    @Operation(
        summary = "Lista os convites (apenas ADMIN)",
        description = "Responde \"quem foi convidado e ainda não entrou\". Sem filtro, traz todos, " +
                "do mais recente para o mais antigo.\n\n" +
                "O `status` devolvido é o **efetivo**: um convite que venceu sem ser usado chega " +
                "como EXPIRADO mesmo estando gravado como PENDENTE — e `?status=EXPIRADO` também " +
                "o encontra. Para a lista de pendências de verdade, use `?status=PENDENTE`."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Convites encontrados"),
        ApiResponse(responseCode = "401", description = "Sem token"),
        ApiResponse(responseCode = "403", description = "Apenas ADMIN")
    )
    @GetMapping
    fun listar(
        @Parameter(description = "Filtra pelo status efetivo do convite")
        @RequestParam(required = false) status: StatusConvite?
    ): ResponseEntity<List<ConviteResumoResponse>> {
        return ResponseEntity.ok(service.listar(status).map { it.toConviteResumoResponse() })
    }

    @Operation(
        summary = "Revoga um convite ainda não utilizado (apenas ADMIN)",
        description = "Para o convite enviado por engano — sem isto, só restava esperar o vencimento. " +
                "O link para de funcionar imediatamente.\n\n" +
                "O registro não é apagado: fica com status REVOGADO, preservando o rastro. Convite já " +
                "utilizado não pode ser revogado (400) — nesse caso o que se quer é inativar o " +
                "usuário em `PATCH /usuarios/{id}/status`."
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Convite revogado"),
        ApiResponse(responseCode = "400", description = "Convite já utilizado"),
        ApiResponse(responseCode = "401", description = "Sem token"),
        ApiResponse(responseCode = "403", description = "Apenas ADMIN"),
        ApiResponse(responseCode = "404", description = "Token inexistente")
    )
    @DeleteMapping("/{token}")
    fun revogar(
        @Parameter(description = "Token do convite a revogar") @PathVariable token: String
    ): ResponseEntity<Void> {
        service.revogar(token)

        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Valida um token de convite (público)",
        description = "Usado pela tela de completar cadastro ao carregar, para preencher o e-mail " +
                "e o perfil sem deixar a pessoa editá-los."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Convite válido"),
        ApiResponse(responseCode = "400", description = "Convite já utilizado ou expirado"),
        ApiResponse(responseCode = "404", description = "Token inexistente")
    )
    @SecurityRequirements
    @GetMapping("/{token}")
    fun validar(
        @Parameter(description = "Token recebido no link do convite") @PathVariable token: String
    ): ResponseEntity<ConviteValidoResponse> {
        val convite = service.buscarPorToken(token)

        return ResponseEntity.ok(convite.toConviteValidoResponse())
    }

    @Operation(
        summary = "Completa o cadastro a partir do convite (público)",
        description = "Cria o usuário com o e-mail e o papel do convite. Quando o convite é de " +
                "PROFESSOR, `telefone` passa a ser obrigatório e a ficha do professor é criada " +
                "aqui (ou reaproveitada, se já houver uma com o mesmo nome e telefone). " +
                "Não emite JWT: em seguida a pessoa faz login normalmente em /auth/login."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Cadastro concluído"),
        ApiResponse(
            responseCode = "400",
            description = "Payload inválido, telefone ausente em convite de PROFESSOR, convite utilizado ou expirado"
        ),
        ApiResponse(responseCode = "404", description = "Token inexistente"),
        ApiResponse(
            responseCode = "409",
            description = "E-mail passou a ter usuário depois do convite, ou a ficha de professor já tem acesso"
        )
    )
    @SecurityRequirements
    @PostMapping("/{token}/completar")
    fun completar(
        @Parameter(description = "Token recebido no link do convite") @PathVariable token: String,
        @Valid @RequestBody request: CompletarCadastroRequest
    ): ResponseEntity<Void> {
        service.completar(token, request)

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}
