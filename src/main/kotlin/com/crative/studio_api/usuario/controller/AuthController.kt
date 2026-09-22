package com.crative.studio_api.usuario.controller

import com.crative.studio_api.shared.security.UsuarioAutenticado
import com.crative.studio_api.usuario.dto.request.LoginRequest
import com.crative.studio_api.usuario.dto.request.TrocarSenhaRequest
import com.crative.studio_api.usuario.dto.response.LoginResponse
import com.crative.studio_api.usuario.exception.CredenciaisInvalidasException
import com.crative.studio_api.usuario.mapper.toLogadoResponse
import com.crative.studio_api.usuario.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Autenticação", description = "Login e emissão de token JWT")
@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {

    @Operation(
        summary = "Autentica um usuário e devolve o token JWT",
        description = "Único endpoint público da API. O token retornado deve ser enviado no header " +
                "Authorization: Bearer <token> nas demais chamadas (ou colado no botão Authorize)."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Autenticado — devolve token e dados do usuário"),
        ApiResponse(responseCode = "401", description = "E-mail inexistente, senha incorreta ou usuário inativo"),
        ApiResponse(responseCode = "400", description = "Payload inválido")
    )
    @SecurityRequirements
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest
    ): ResponseEntity<LoginResponse> {

        val resultado = authService.autenticar(
            request.email,
            request.senha
        )

        return ResponseEntity.ok(
            LoginResponse(
                token = resultado.token,
                usuario = resultado.usuario.toLogadoResponse()
            )
        )
    }

    @Operation(
        summary = "Troca a senha do usuário logado",
        description = "Age sempre sobre a **própria** conta: o id sai do token, não do corpo, então " +
                "não há como trocar a senha de outra pessoa por aqui.\n\n" +
                "Exigir `senhaAtual` é o que impede que um token vazado vire posse permanente da conta.\n\n" +
                "Senha atual incorreta responde **400**, não 401: o usuário está autenticado, o que " +
                "está errado é o payload — devolver 401 faria o interceptor do front deslogar quem " +
                "só errou a digitação.\n\n" +
                "Não emite token novo; o JWT em uso continua valendo até expirar."
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Senha alterada"),
        ApiResponse(
            responseCode = "400",
            description = "Senha atual incorreta, nova senha igual à atual, ou com menos de 8 caracteres"
        ),
        ApiResponse(responseCode = "401", description = "Sem token, token inválido ou usuário inativado")
    )
    @PostMapping("/trocar-senha")
    fun trocarSenha(
        @Valid @RequestBody request: TrocarSenhaRequest
    ): ResponseEntity<Void> {

        val usuarioId = UsuarioAutenticado.id()
            ?: throw CredenciaisInvalidasException("Autenticação necessária")

        authService.trocarSenha(usuarioId, request.senhaAtual, request.novaSenha)

        return ResponseEntity.noContent().build()
    }
}
