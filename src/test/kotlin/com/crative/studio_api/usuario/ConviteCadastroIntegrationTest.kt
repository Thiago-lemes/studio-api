package com.crative.studio_api.usuario

import com.crative.studio_api.shared.AbstractIntegrationTest
import com.crative.studio_api.usuario.entity.RoleType
import com.crative.studio_api.usuario.entity.StatusConvite
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID

class ConviteCadastroIntegrationTest : AbstractIntegrationTest() {

    private fun criarConviteViaApi(
        corpo: String,
        autorizacao: String? = tokenDe(RoleType.ADMIN)
    ): ResultActions = mockMvc.perform(
        post("/usuarios/convites")
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo)
            .apply { autorizacao?.let { header("Authorization", it) } }
    )

    private fun completar(
        token: String,
        nome: String,
        senha: String,
        telefone: String? = null,
        especialidade: String? = null
    ): ResultActions {
        val campos = buildString {
            append("""{"nome":"$nome","senha":"$senha"""")
            telefone?.let { append(""","telefone":"$it"""") }
            especialidade?.let { append(""","especialidade":"$it"""") }
            append("}")
        }

        return mockMvc.perform(
            post("/usuarios/convites/$token/completar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(campos)
        )
    }

    private fun tokenDoConvite(resposta: String) = JsonPath.read<String>(resposta, "$.token")

    @Test
    fun fluxo_completo_do_convite_ate_o_login() {
        val resposta = criarConviteViaApi("""{"email":"ana@studio.com","role":"SECRETARIA"}""")
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("PENDENTE"))
            .andExpect(jsonPath("$.linkConvite").isNotEmpty)
            .andReturn().response.contentAsString

        val token = tokenDoConvite(resposta)
        val link = JsonPath.read<String>(resposta, "$.linkConvite")
        assertTrue(link.endsWith("/completar-cadastro?token=$token"), "link deveria carregar o token: $link")

        mockMvc.perform(get("/usuarios/convites/$token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("ana@studio.com"))
            .andExpect(jsonPath("$.role").value("SECRETARIA"))

        completar(token, "Ana Souza", "senhaSegura1").andExpect(status().isCreated)

        assertEquals(StatusConvite.UTILIZADO, conviteUsuarioRepository.findByToken(token)!!.status)

        val usuario = usuarioRepository.findByEmail("ana@studio.com")
        assertNotNull(usuario)
        assertEquals("Ana Souza", usuario!!.nome)

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"ana@studio.com","senha":"senhaSegura1"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").isNotEmpty)
    }

    /** O ADMIN informa só o e-mail; a ficha do professor nasce com o que a própria pessoa preenche. */
    @Test
    fun convite_de_professor_cria_a_ficha_e_o_usuario_vinculado() {
        val resposta = criarConviteViaApi("""{"email":"prof@studio.com","role":"PROFESSOR"}""")
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString

        completar(
            tokenDoConvite(resposta), "Prof Marina", "senhaSegura1",
            telefone = "11999990000", especialidade = "Ballet"
        ).andExpect(status().isCreated)

        val professor = professorRepository.findByNomeAndTelefone("Prof Marina", "11999990000")
        assertNotNull(professor)
        assertEquals("Ballet", professor!!.especialidade)

        // professor_id preenchido prova que o chk_usuario_professor da V2 passou
        assertEquals(professor.id, usuarioRepository.findByEmail("prof@studio.com")!!.professorId)
    }

    @Test
    fun deve_recusar_completar_convite_de_professor_sem_telefone() {
        val convite = criarConvite(email = "prof@studio.com", role = RoleType.PROFESSOR)

        completar(convite.token, "Prof Marina", "senhaSegura1").andExpect(status().isBadRequest)

        // nada foi gravado: nem usuário, nem ficha, e o convite segue utilizável
        assertEquals(null, usuarioRepository.findByEmail("prof@studio.com"))
        assertEquals(StatusConvite.PENDENTE, conviteUsuarioRepository.findByToken(convite.token)!!.status)
    }

    /** Professor que já dava aula e só agora ganha login: a ficha existente é reaproveitada. */
    @Test
    fun deve_reaproveitar_ficha_de_professor_ja_cadastrado() {
        val existente = criarProfessor(nome = "Prof Marina", telefone = "11999990000")
        val convite = criarConvite(email = "prof@studio.com", role = RoleType.PROFESSOR)

        completar(convite.token, "Prof Marina", "senhaSegura1", telefone = "11999990000")
            .andExpect(status().isCreated)

        assertEquals(existente.id, usuarioRepository.findByEmail("prof@studio.com")!!.professorId)
        assertEquals(1, professorRepository.findAllByAtivoTrue().size)
    }

    @Test
    fun deve_recusar_quando_a_ficha_de_professor_ja_tem_acesso() {
        val existente = criarProfessor(nome = "Prof Marina", telefone = "11999990000")
        criarUsuario(email = "marina@studio.com", role = RoleType.PROFESSOR, professorId = existente.id)
        val convite = criarConvite(email = "outra@studio.com", role = RoleType.PROFESSOR)

        completar(convite.token, "Prof Marina", "senhaSegura1", telefone = "11999990000")
            .andExpect(status().isConflict)

        assertEquals(null, usuarioRepository.findByEmail("outra@studio.com"))
    }

    @Test
    fun criar_convite_exige_admin() {
        criarConviteViaApi("""{"email":"ana@studio.com","role":"SECRETARIA"}""", autorizacao = null)
            .andExpect(status().isUnauthorized)

        criarConviteViaApi(
            """{"email":"ana@studio.com","role":"SECRETARIA"}""",
            autorizacao = tokenDe(RoleType.SECRETARIA)
        ).andExpect(status().isForbidden)
    }

    @Test
    fun os_endpoints_do_token_sao_publicos() {
        val convite = criarConvite(email = "ana@studio.com")

        mockMvc.perform(get("/usuarios/convites/${convite.token}")).andExpect(status().isOk)
        completar(convite.token, "Ana Souza", "senhaSegura1").andExpect(status().isCreated)
    }

    @Test
    fun deve_recusar_token_inexistente() {
        mockMvc.perform(get("/usuarios/convites/${UUID.randomUUID()}")).andExpect(status().isNotFound)
        completar(UUID.randomUUID().toString(), "Ana", "senhaSegura1").andExpect(status().isNotFound)
    }

    @Test
    fun nao_deve_permitir_usar_o_mesmo_convite_duas_vezes() {
        val convite = criarConvite(email = "ana@studio.com")

        completar(convite.token, "Ana Souza", "senhaSegura1").andExpect(status().isCreated)
        completar(convite.token, "Outra Ana", "senhaSegura2").andExpect(status().isBadRequest)

        mockMvc.perform(get("/usuarios/convites/${convite.token}")).andExpect(status().isBadRequest)
    }

    @Test
    fun deve_recusar_convite_vencido() {
        val convite = criarConvite(expiraEm = LocalDateTime.now().minusMinutes(1))

        mockMvc.perform(get("/usuarios/convites/${convite.token}")).andExpect(status().isBadRequest)
        completar(convite.token, "Ana Souza", "senhaSegura1").andExpect(status().isBadRequest)

        assertEquals(null, usuarioRepository.findByEmail(convite.email))
    }

    @Test
    fun reenviar_convite_invalida_o_anterior() {
        val primeiro = criarConvite(email = "ana@studio.com")

        criarConviteViaApi("""{"email":"ana@studio.com","role":"SECRETARIA"}""")
            .andExpect(status().isCreated)

        assertEquals(StatusConvite.EXPIRADO, conviteUsuarioRepository.findByToken(primeiro.token)!!.status)
        mockMvc.perform(get("/usuarios/convites/${primeiro.token}")).andExpect(status().isBadRequest)
    }

    @Test
    fun deve_recusar_convite_para_email_que_ja_tem_usuario() {
        criarUsuario(email = "ana@studio.com")

        criarConviteViaApi("""{"email":"ana@studio.com","role":"SECRETARIA"}""")
            .andExpect(status().isConflict)
    }

    @Test
    fun deve_recusar_email_invalido_na_criacao_do_convite() {
        criarConviteViaApi("""{"email":"nao-e-email","role":"SECRETARIA"}""")
            .andExpect(status().isBadRequest)
    }

    @Test
    fun deve_recusar_senha_curta_no_completar() {
        val convite = criarConvite()

        completar(convite.token, "Ana Souza", "curta").andExpect(status().isBadRequest)
    }
}
