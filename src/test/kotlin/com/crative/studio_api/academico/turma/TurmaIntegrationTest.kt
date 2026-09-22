package com.crative.studio_api.academico.turma

import com.crative.studio_api.shared.AbstractIntegrationTest
import com.crative.studio_api.usuario.entity.RoleType
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalTime
import java.util.UUID

class TurmaIntegrationTest : AbstractIntegrationTest() {

    private val auth by lazy { tokenDe() }

    private fun criar(
        professorId: UUID,
        salaId: UUID,
        dias: String = """["SEG"]""",
        inicio: String = "09:00:00",
        fim: String = "10:00:00",
        capacidade: Int = 10,
        modalidade: String = "Ballet"
    ): ResultActions = mockMvc.perform(
        post("/turmas")
            .header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                  "modalidade":"$modalidade",
                  "professorId":"$professorId",
                  "salaId":"$salaId",
                  "diasSemana":$dias,
                  "horarioInicio":"$inicio",
                  "horarioFim":"$fim",
                  "capacidadeMaxima":$capacidade
                }
                """.trimIndent()
            )
    )

    @Test
    fun deve_criar_turma_persistindo_os_dias_da_semana() {
        val professor = criarProfessor()
        val sala = criarSala()

        val resultado = criar(professor.id!!, sala.id!!, dias = """["SEG","QUA","SEX"]""")
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.professorNome").value(professor.nome))
            .andExpect(jsonPath("$.salaNome").value(sala.nome))
            .andExpect(jsonPath("$.vagasDisponiveis").value(10))
            .andExpect(jsonPath("$.diasSemana.length()").value(3))

        val id = UUID.fromString(JsonPath.read(resultado.andReturn().response.contentAsString, "$.id"))
        val naBase = turmaRepository.findById(id).orElseThrow()
        assertEquals(3, naBase.diasSemana.size)
    }

    @Test
    fun choque_de_horario_na_mesma_sala_devolve_409() {
        val professorA = criarProfessor(nome = "Marina", telefone = "1")
        val professorB = criarProfessor(nome = "João", telefone = "2")
        val sala = criarSala()

        criar(professorA.id!!, sala.id!!, inicio = "09:00:00", fim = "10:00:00")
            .andExpect(status().isCreated)
        criar(professorB.id!!, sala.id!!, inicio = "09:30:00", fim = "10:30:00")
            .andExpect(status().isConflict)

        assertEquals(1, turmaRepository.count())
    }

    @Test
    fun turmas_encostadas_na_mesma_sala_sao_aceitas() {
        val professorA = criarProfessor(nome = "Marina", telefone = "1")
        val professorB = criarProfessor(nome = "João", telefone = "2")
        val sala = criarSala()

        criar(professorA.id!!, sala.id!!, inicio = "09:00:00", fim = "10:00:00")
            .andExpect(status().isCreated)
        criar(professorB.id!!, sala.id!!, inicio = "10:00:00", fim = "11:00:00")
            .andExpect(status().isCreated)

        assertEquals(2, turmaRepository.count())
    }

    @Test
    fun mesmo_horario_em_salas_diferentes_e_aceito() {
        val professorA = criarProfessor(nome = "Marina", telefone = "1")
        val professorB = criarProfessor(nome = "João", telefone = "2")

        criar(professorA.id!!, criarSala(nome = "Sala 1").id!!).andExpect(status().isCreated)
        criar(professorB.id!!, criarSala(nome = "Sala 2").id!!).andExpect(status().isCreated)
    }

    @Test
    fun mesmo_professor_em_salas_diferentes_no_mesmo_horario_devolve_409() {
        val professor = criarProfessor()

        criar(professor.id!!, criarSala(nome = "Sala 1").id!!).andExpect(status().isCreated)
        criar(professor.id!!, criarSala(nome = "Sala 2").id!!).andExpect(status().isConflict)
    }

    @Test
    fun mesmo_horario_em_dias_diferentes_e_aceito() {
        val professor = criarProfessor()
        val sala = criarSala()

        criar(professor.id!!, sala.id!!, dias = """["SEG"]""").andExpect(status().isCreated)
        criar(professor.id!!, sala.id!!, dias = """["TER"]""").andExpect(status().isCreated)
    }

    @Test
    fun atualizar_mantendo_os_proprios_horarios_e_aceito() {
        val professor = criarProfessor()
        val sala = criarSala()
        val turma = criarTurma(professorId = professor.id!!, salaId = sala.id!!)

        mockMvc.perform(
            put("/turmas/${turma.id}")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "professorId":"${professor.id}",
                      "salaId":"${sala.id}",
                      "diasSemana":["SEG"],
                      "horarioInicio":"09:00:00",
                      "horarioFim":"11:00:00",
                      "capacidadeMaxima":15
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.capacidadeMaxima").value(15))

        assertEquals(LocalTime.of(11, 0), turmaRepository.findById(turma.id!!).orElseThrow().horarioFim)
    }

    @Test
    fun professor_inexistente_devolve_404() {
        criar(UUID.randomUUID(), criarSala().id!!)
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.mensagem").value("Professor não encontrado"))

        assertEquals(0, turmaRepository.count())
    }

    @Test
    fun sala_inexistente_devolve_404() {
        criar(criarProfessor().id!!, UUID.randomUUID())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.mensagem").value("Sala não encontrada"))

        assertEquals(0, turmaRepository.count())
    }

    @Test
    fun atualizar_apontando_para_professor_inexistente_devolve_404() {
        val turma = criarTurma(professorId = criarProfessor().id!!, salaId = criarSala().id!!)

        mockMvc.perform(
            put("/turmas/${turma.id}")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "professorId":"${UUID.randomUUID()}",
                      "salaId":"${turma.salaId}",
                      "diasSemana":["SEG"],
                      "horarioInicio":"09:00:00",
                      "horarioFim":"10:00:00",
                      "capacidadeMaxima":10
                    }
                    """.trimIndent()
                )
        ).andExpect(status().isNotFound)

        // a turma original fica intacta
        assertEquals(turma.professorId, turmaRepository.findById(turma.id!!).orElseThrow().professorId)
    }

    @Test
    fun dias_da_semana_vazio_devolve_400() {
        criar(criarProfessor().id!!, criarSala().id!!, dias = "[]").andExpect(status().isBadRequest)
    }

    @Test
    fun capacidade_nao_positiva_devolve_400() {
        criar(criarProfessor().id!!, criarSala().id!!, capacidade = 0).andExpect(status().isBadRequest)
    }

    @Test
    fun a_listagem_traz_so_as_turmas_ativas() {
        val professor = criarProfessor()
        criarTurma(professorId = professor.id!!, salaId = criarSala(nome = "Sala 1").id!!, modalidade = "Ativa")
        criarTurma(
            professorId = professor.id!!, salaId = criarSala(nome = "Sala 2").id!!,
            modalidade = "Inativa", ativa = false
        )

        mockMvc.perform(get("/turmas").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].modalidade").value("Ativa"))
    }

    @Test
    fun buscar_turma_inexistente_devolve_404() {
        mockMvc.perform(get("/turmas/${UUID.randomUUID()}").header("Authorization", auth))
            .andExpect(status().isNotFound)
    }

    // ---------- recorte do professor ----------

    @Test
    fun professor_ve_os_alunos_da_propria_turma() {
        val professor = criarProfessor()
        val turma = criarTurma(professorId = professor.id!!, salaId = criarSala().id!!)

        mockMvc.perform(
            get("/turmas/${turma.id}/alunos")
                .header("Authorization", tokenDe(RoleType.PROFESSOR, professor.id))
        ).andExpect(status().isOk)
    }

    @Test
    fun professor_nao_ve_os_alunos_da_turma_de_outro_professor() {
        val dono = criarProfessor(nome = "Marina", telefone = "1")
        val intruso = criarProfessor(nome = "João", telefone = "2")
        val turma = criarTurma(professorId = dono.id!!, salaId = criarSala().id!!)

        mockMvc.perform(
            get("/turmas/${turma.id}/alunos")
                .header("Authorization", tokenDe(RoleType.PROFESSOR, intruso.id))
        ).andExpect(status().isForbidden)
    }

    @Test
    fun secretaria_ve_os_alunos_de_qualquer_turma() {
        val professor = criarProfessor()
        val turma = criarTurma(professorId = professor.id!!, salaId = criarSala().id!!)

        mockMvc.perform(get("/turmas/${turma.id}/alunos").header("Authorization", auth))
            .andExpect(status().isOk)
    }
}
