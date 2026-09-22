package com.crative.studio_api.usuario

import com.crative.studio_api.shared.AbstractIntegrationTest
import com.crative.studio_api.usuario.entity.RoleType
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AuthIntegrationTest : AbstractIntegrationTest() {

    private fun login(email: String, senha: String) =
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","senha":"$senha"}""")
        )

    @Test
    fun deve_autenticar_e_devolver_token_com_os_dados_do_usuario() {
        criarUsuario(email = "ana@studio.com", senha = "senha123", role = RoleType.SECRETARIA)

        login("ana@studio.com", "senha123")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").isNotEmpty)
            .andExpect(jsonPath("$.usuario.email").value("ana@studio.com"))
            .andExpect(jsonPath("$.usuario.role").value("SECRETARIA"))
    }

    @Test
    fun o_token_emitido_no_login_abre_os_endpoints_protegidos() {
        criarUsuario(email = "ana@studio.com", senha = "senha123")

        val corpo = login("ana@studio.com", "senha123")
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        val token = com.jayway.jsonpath.JsonPath.read<String>(corpo, "$.token")

        mockMvc.perform(get("/alunos").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
    }

    @Test
    fun deve_recusar_senha_incorreta() {
        criarUsuario(email = "ana@studio.com", senha = "senha123")

        login("ana@studio.com", "senhaErrada").andExpect(status().isUnauthorized)
    }

    @Test
    fun deve_recusar_email_inexistente() {
        login("naoexiste@studio.com", "senha123").andExpect(status().isUnauthorized)
    }

    @Test
    fun deve_recusar_usuario_inativo() {
        criarUsuario(email = "ana@studio.com", senha = "senha123", ativo = false)

        login("ana@studio.com", "senha123").andExpect(status().isUnauthorized)
    }

    @Test
    fun deve_recusar_payload_invalido() {
        login("nao-e-email", "senha123").andExpect(status().isBadRequest)
    }

    @Test
    fun a_senha_nunca_e_devolvida_na_resposta_do_login() {
        criarUsuario(email = "ana@studio.com", senha = "senha123")

        val corpo = login("ana@studio.com", "senha123")
            .andReturn().response.contentAsString

        assert(!corpo.contains("senha123")) { "a senha em claro vazou na resposta" }
        assert(!corpo.contains("senhaHash")) { "o hash da senha vazou na resposta" }
    }

    // ---------- autorização ----------
    //
    // A API separa os dois casos: 401 para quem não apresentou credencial válida (o
    // AuthenticationEntryPoint do SecurityConfig) e 403 para quem se autenticou mas cujo papel
    // não alcança o recurso.

    @Test
    fun endpoint_protegido_sem_token_devolve_401() {
        mockMvc.perform(get("/alunos"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.mensagem").isNotEmpty)
    }

    @Test
    fun token_invalido_e_tratado_como_ausente() {
        mockMvc.perform(get("/alunos").header("Authorization", "Bearer token-falsificado"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun header_sem_o_prefixo_bearer_e_ignorado() {
        mockMvc.perform(get("/alunos").header("Authorization", "Basic YWRtaW46YWRtaW4="))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun token_expirado_nao_autentica() {
        // assinado com a mesma chave, mas já vencido
        val expirado = com.crative.studio_api.shared.security.JwtService(
            secret = "segredo-de-teste-com-mais-de-trinta-e-dois-caracteres-ok",
            expirationMs = -1_000
        ).gerarToken(java.util.UUID.randomUUID(), RoleType.ADMIN, null)

        mockMvc.perform(get("/alunos").header("Authorization", "Bearer $expirado"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun token_assinado_com_outra_chave_nao_autentica() {
        val outraChave = com.crative.studio_api.shared.security.JwtService(
            secret = "uma-chave-completamente-diferente-com-32-caracteres+",
            expirationMs = 60_000
        ).gerarToken(java.util.UUID.randomUUID(), RoleType.ADMIN, null)

        mockMvc.perform(get("/alunos").header("Authorization", "Bearer $outraChave"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun professor_pode_ler_turmas_e_alunos() {
        val professor = criarProfessor()

        mockMvc.perform(get("/alunos").header("Authorization", tokenDe(RoleType.PROFESSOR, professor.id)))
            .andExpect(status().isOk)
        mockMvc.perform(get("/turmas").header("Authorization", tokenDe(RoleType.PROFESSOR, professor.id)))
            .andExpect(status().isOk)
    }

    @Test
    fun professor_nao_pode_escrever_no_financeiro() {
        val professor = criarProfessor()

        mockMvc.perform(
            get("/contas-receber").header("Authorization", tokenDe(RoleType.PROFESSOR, professor.id))
        ).andExpect(status().isForbidden)
    }

    @Test
    fun professor_nao_pode_criar_aluno() {
        val professor = criarProfessor()

        mockMvc.perform(
            post("/alunos")
                .header("Authorization", tokenDe(RoleType.PROFESSOR, professor.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"nome":"Joana","telefone":null,"dataNascimento":"2000-01-01","cpf":"111.111.111-11"}
                    """.trimIndent()
                )
        ).andExpect(status().isForbidden)
    }

    @Test
    fun admin_e_secretaria_podem_ler_o_financeiro() {
        mockMvc.perform(get("/contas-receber").header("Authorization", tokenDe(RoleType.ADMIN)))
            .andExpect(status().isOk)
        mockMvc.perform(get("/contas-receber").header("Authorization", tokenDe(RoleType.SECRETARIA)))
            .andExpect(status().isOk)
    }

    @Test
    fun o_login_e_publico() {
        // sem Authorization e ainda assim entra no controller (401 de credencial, não de acesso)
        login("ninguem@studio.com", "x").andExpect(status().isUnauthorized)
    }
}
