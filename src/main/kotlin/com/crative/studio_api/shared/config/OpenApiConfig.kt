package com.crative.studio_api.shared.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Documentação em `/swagger-ui.html` (spec crua em `/v3/api-docs`).
 *
 * O `bearerAuth` é declarado como requisito global: todo endpoint aparece como protegido,
 * e os poucos públicos (`POST /auth/login`) se marcam individualmente com
 * `@SecurityRequirements` pra limpar o cadeado.
 *
 * Fluxo de uso: `POST /auth/login` → copiar o `token` da resposta → botão **Authorize** →
 * colar só o token (o "Bearer " é adicionado pelo próprio Swagger UI).
 */
@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI {
        val esquemaJwt = SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("Token JWT obtido em POST /auth/login. Cole apenas o token, sem o prefixo 'Bearer'.")

        return OpenAPI()
            .info(
                Info()
                    .title("EduManager API")
                    .version("v1")
                    .description(
                        """
                        API de gestão de estúdio/escola: alunos e responsáveis, professores, salas,
                        turmas com controle de choque de horário, matrículas e financeiro
                        (contas a pagar/receber, pagamentos e dashboard).

                        **Autenticação:** todos os endpoints exigem JWT, exceto POST /auth/login.
                        **Perfis:** ADMIN e SECRETARIA têm acesso amplo; PROFESSOR só lê dados
                        acadêmicos e não acessa o módulo financeiro.
                        """.trimIndent()
                    )
            )
            .addServersItem(Server().url("/").description("Servidor atual"))
            .components(Components().addSecuritySchemes(SEGURANCA_JWT, esquemaJwt))
            .addSecurityItem(SecurityRequirement().addList(SEGURANCA_JWT))
    }

    companion object {
        const val SEGURANCA_JWT = "bearerAuth"
    }
}
