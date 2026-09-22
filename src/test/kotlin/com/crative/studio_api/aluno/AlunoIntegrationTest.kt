package com.crative.studio_api.aluno

import com.crative.studio_api.shared.AbstractIntegrationTest
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.util.UUID

class AlunoIntegrationTest : AbstractIntegrationTest() {

    private val auth by lazy { tokenDe() }

    private fun cadastrar(
        nome: String = "Joana",
        cpf: String = "111.111.111-11",
        dataNascimento: LocalDate = LocalDate.now().minusYears(20)
    ): ResultActions = mockMvc.perform(
        post("/alunos")
            .header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"nome":"$nome","telefone":"11988887777","dataNascimento":"$dataNascimento","cpf":"$cpf"}
                """.trimIndent()
            )
    )

    private fun idDe(resultado: ResultActions): String =
        JsonPath.read(resultado.andReturn().response.contentAsString, "$.id")

    @Test
    fun deve_cadastrar_aluno_e_persistir_no_banco() {
        val resultado = cadastrar(nome = "Joana", dataNascimento = LocalDate.now().minusYears(20))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.nome").value("Joana"))
            .andExpect(jsonPath("$.idade").value(20))
            .andExpect(jsonPath("$.menorDeIdade").value(false))
            .andExpect(jsonPath("$.ativo").value(true))

        val id = UUID.fromString(idDe(resultado))
        assertEquals("Joana", alunoRepository.findById(id).orElseThrow().nome)
    }

    @Test
    fun deve_marcar_menor_de_idade_no_cadastro() {
        cadastrar(nome = "Pedrinho", cpf = "222.222.222-22", dataNascimento = LocalDate.now().minusYears(8))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.idade").value(8))
            .andExpect(jsonPath("$.menorDeIdade").value(true))
    }

    @Test
    fun cpf_duplicado_devolve_409() {
        cadastrar(cpf = "111.111.111-11").andExpect(status().isCreated)

        cadastrar(nome = "Outra", cpf = "111.111.111-11")
            .andExpect(status().isConflict)

        assertEquals(1, alunoRepository.count())
    }

    @Test
    fun data_de_nascimento_futura_devolve_400() {
        cadastrar(dataNascimento = LocalDate.now().plusYears(1))
            .andExpect(status().isBadRequest)

        assertEquals(0, alunoRepository.count())
    }

    @Test
    fun nome_em_branco_devolve_400_pela_validacao_do_payload() {
        mockMvc.perform(
            post("/alunos")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nome":"  ","telefone":null,"dataNascimento":"2000-01-01","cpf":"111.111.111-11"}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun deve_buscar_aluno_por_id() {
        val aluno = criarAluno(nome = "Joana")

        mockMvc.perform(get("/alunos/${aluno.id}").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nome").value("Joana"))
    }

    @Test
    fun buscar_id_inexistente_devolve_404() {
        mockMvc.perform(get("/alunos/${UUID.randomUUID()}").header("Authorization", auth))
            .andExpect(status().isNotFound)
    }

    @Test
    fun id_malformado_devolve_400() {
        mockMvc.perform(get("/alunos/nao-e-uuid").header("Authorization", auth))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun a_listagem_traz_so_os_ativos() {
        criarAluno(nome = "Ativa", cpf = "111.111.111-11")
        criarAluno(nome = "Inativa", cpf = "222.222.222-22", ativo = false)

        mockMvc.perform(get("/alunos").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].nome").value("Ativa"))
    }

    @Test
    fun deve_atualizar_nome_e_telefone_preservando_cpf_e_nascimento() {
        val aluno = criarAluno(nome = "Nome Antigo", cpf = "111.111.111-11")

        mockMvc.perform(
            put("/alunos/${aluno.id}")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nome":"Nome Novo","telefone":"11900000000"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nome").value("Nome Novo"))
            .andExpect(jsonPath("$.telefone").value("11900000000"))
            .andExpect(jsonPath("$.cpf").value("111.111.111-11"))
    }

    @Test
    fun atualizar_aluno_inexistente_devolve_404() {
        mockMvc.perform(
            put("/alunos/${UUID.randomUUID()}")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nome":"Nome","telefone":null}""")
        ).andExpect(status().isNotFound)
    }

    @Test
    fun o_delete_e_um_soft_delete() {
        val aluno = criarAluno()

        mockMvc.perform(delete("/alunos/${aluno.id}").header("Authorization", auth))
            .andExpect(status().isNoContent)

        val naBase = alunoRepository.findById(aluno.id!!).orElseThrow()
        assertFalse(naBase.ativo, "o registro deveria continuar na base, apenas inativo")
    }

    @Test
    fun deve_reativar_aluno_pelo_patch_de_status() {
        val aluno = criarAluno(ativo = false)

        mockMvc.perform(
            patch("/alunos/${aluno.id}/status")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"ativo":true}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.ativo").value(true))
    }

    @Test
    fun o_aluno_inativado_sai_da_listagem_e_continua_acessivel_por_id() {
        val aluno = criarAluno(nome = "Joana")

        mockMvc.perform(delete("/alunos/${aluno.id}").header("Authorization", auth))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/alunos").header("Authorization", auth))
            .andExpect(jsonPath("$.length()").value(0))
        mockMvc.perform(get("/alunos/${aluno.id}").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.ativo").value(false))
    }
}
