package com.crative.studio_api.academico.matricula

import com.crative.studio_api.academico.matricula.types.StatusMatriculaType
import com.crative.studio_api.financeiro.types.StatusContaType
import com.crative.studio_api.shared.AbstractIntegrationTest
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * O caminho que o dinheiro percorre: matricular gera a conta a receber da competência de
 * início, e registrar o pagamento quita essa conta — tudo contra o Postgres real, para que
 * as constraints (UNIQUE de matrícula e de competência) participem do teste.
 */
class MatriculaFluxoIntegrationTest : AbstractIntegrationTest() {

    private val auth by lazy { tokenDe() }

    private fun matricular(
        alunoId: UUID,
        turmaId: UUID,
        valor: String = "300.00",
        diaVencimento: Int = 10,
        desconto: String = "0.00",
        dataInicio: LocalDate = LocalDate.now()
    ): ResultActions = mockMvc.perform(
        post("/matriculas")
            .header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                  "alunoId":"$alunoId",
                  "turmaId":"$turmaId",
                  "valorMensalidade":$valor,
                  "diaVencimento":$diaVencimento,
                  "descontoPercentual":$desconto,
                  "dataInicio":"$dataInicio"
                }
                """.trimIndent()
            )
    )

    private fun idDe(resultado: ResultActions): UUID =
        UUID.fromString(JsonPath.read(resultado.andReturn().response.contentAsString, "$.id"))

    @Test
    fun matricular_gera_a_cobranca_da_competencia_de_inicio() {
        val aluno = criarAluno()
        val turma = criarTurma()
        val inicio = LocalDate.of(2026, 3, 20)

        val matriculaId = idDe(
            matricular(aluno.id!!, turma.id!!, valor = "400.00", diaVencimento = 5, dataInicio = inicio)
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.status").value("ATIVA"))
                .andExpect(jsonPath("$.valorEfetivo").value(400.00))
        )

        val cobrancas = contaReceberRepository.findAllByMatriculaId(matriculaId)
        assertEquals(1, cobrancas.size)
        val cobranca = cobrancas.single()
        assertEquals("2026-03", cobranca.referencia)
        assertEquals(LocalDate.of(2026, 3, 5), cobranca.vencimento)
        assertEquals(BigDecimal("400.00"), cobranca.valor)
    }

    @Test
    fun a_cobranca_nasce_com_o_valor_ja_descontado() {
        val aluno = criarAluno()
        val turma = criarTurma()

        val matriculaId = idDe(
            matricular(aluno.id!!, turma.id!!, valor = "400.00", desconto = "25.00")
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.valorMensalidade").value(400.00))
                .andExpect(jsonPath("$.valorEfetivo").value(300.00))
        )

        assertEquals(
            BigDecimal("300.00"),
            contaReceberRepository.findAllByMatriculaId(matriculaId).single().valor
        )
    }

    @Test
    fun fluxo_completo_da_matricula_ao_pagamento() {
        val aluno = criarAluno()
        val turma = criarTurma()

        // competência à frente para o vencimento cair no futuro e a conta nascer PENDENTE
        // independentemente do dia em que a suíte roda
        val matriculaId = idDe(
            matricular(aluno.id!!, turma.id!!, dataInicio = LocalDate.now().plusMonths(2))
                .andExpect(status().isCreated)
        )
        val conta = contaReceberRepository.findAllByMatriculaId(matriculaId).single()
        assertEquals(StatusContaType.PENDENTE, conta.status)

        // a conta aparece no extrato do aluno já detalhada
        mockMvc.perform(
            get("/contas-receber").param("matriculaId", matriculaId.toString())
                .header("Authorization", auth)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].nomeAluno").value(aluno.nome))

        mockMvc.perform(
            post("/pagamentos")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"contaReceberId":"${conta.id}","valorPago":300.00,"formaPagamento":"PIX"}
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.formaPagamento").value("PIX"))

        assertEquals(
            StatusContaType.PAGO,
            contaReceberRepository.findById(conta.id!!).orElseThrow().status
        )
        assertEquals(1, pagamentoRepository.findAllByContaReceberId(conta.id!!).size)
    }

    @Test
    fun pagar_a_mesma_conta_duas_vezes_devolve_409_e_nao_duplica_o_pagamento() {
        val aluno = criarAluno()
        val turma = criarTurma()
        val matriculaId = idDe(matricular(aluno.id!!, turma.id!!).andExpect(status().isCreated))
        val conta = contaReceberRepository.findAllByMatriculaId(matriculaId).single()

        val pagamento = """{"contaReceberId":"${conta.id}","valorPago":300.00,"formaPagamento":"PIX"}"""
        val requisicao = {
            mockMvc.perform(
                post("/pagamentos")
                    .header("Authorization", auth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(pagamento)
            )
        }

        requisicao().andExpect(status().isCreated)
        requisicao().andExpect(status().isConflict)

        assertEquals(1, pagamentoRepository.findAllByContaReceberId(conta.id!!).size)
    }

    @Test
    fun aluno_ja_matriculado_na_turma_devolve_409() {
        val aluno = criarAluno()
        val turma = criarTurma()

        matricular(aluno.id!!, turma.id!!).andExpect(status().isCreated)
        matricular(aluno.id!!, turma.id!!).andExpect(status().isConflict)

        assertEquals(1, matriculaRepository.findAllByAlunoId(aluno.id!!).size)
    }

    /**
     * A tabela tem UNIQUE(aluno_id, turma_id): a rematrícula precisa reaproveitar a linha
     * cancelada, senão o INSERT estoura no banco.
     */
    @Test
    fun rematricula_apos_cancelamento_reaproveita_a_linha_existente() {
        val aluno = criarAluno()
        val turma = criarTurma()

        val matriculaId = idDe(
            matricular(aluno.id!!, turma.id!!, valor = "300.00").andExpect(status().isCreated)
        )

        mockMvc.perform(
            patch("/matriculas/$matriculaId/status")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"CANCELADA"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.dataFim").value(LocalDate.now().toString()))

        val rematriculaId = idDe(
            matricular(aluno.id!!, turma.id!!, valor = "450.00", dataInicio = LocalDate.now().plusMonths(1))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.status").value("ATIVA"))
                .andExpect(jsonPath("$.valorMensalidade").value(450.00))
        )

        assertEquals(matriculaId, rematriculaId, "a rematrícula deveria reusar o mesmo registro")
        assertEquals(1, matriculaRepository.count())
        assertNull(matriculaRepository.findById(matriculaId).orElseThrow().dataFim)
    }

    @Test
    fun a_rematricula_gera_a_cobranca_da_nova_competencia_sem_apagar_a_antiga() {
        val aluno = criarAluno()
        val turma = criarTurma()
        val primeiroInicio = LocalDate.of(2026, 2, 1)
        val novoInicio = LocalDate.of(2026, 6, 1)

        val matriculaId = idDe(
            matricular(aluno.id!!, turma.id!!, dataInicio = primeiroInicio).andExpect(status().isCreated)
        )
        mockMvc.perform(
            patch("/matriculas/$matriculaId/status")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"CANCELADA"}""")
        ).andExpect(status().isOk)

        matricular(aluno.id!!, turma.id!!, dataInicio = novoInicio).andExpect(status().isCreated)

        val referencias = contaReceberRepository.findAllByMatriculaId(matriculaId).map { it.referencia }
        assertEquals(setOf("2026-02", "2026-06"), referencias.toSet())
    }

    @Test
    fun turma_sem_vaga_devolve_400() {
        val turma = criarTurma(capacidadeMaxima = 1)
        val primeiro = criarAluno(nome = "Primeira", cpf = "111.111.111-11")
        val segundo = criarAluno(nome = "Segunda", cpf = "222.222.222-22")

        matricular(primeiro.id!!, turma.id!!).andExpect(status().isCreated)
        matricular(segundo.id!!, turma.id!!).andExpect(status().isBadRequest)

        assertEquals(1, matriculaRepository.count())
        // a matrícula recusada não pode deixar cobrança órfã para trás
        assertEquals(1, contaReceberRepository.count())
    }

    @Test
    fun cancelar_libera_a_vaga_para_outro_aluno() {
        val turma = criarTurma(capacidadeMaxima = 1)
        val primeiro = criarAluno(nome = "Primeira", cpf = "111.111.111-11")
        val segundo = criarAluno(nome = "Segunda", cpf = "222.222.222-22")

        val primeiraMatricula = idDe(matricular(primeiro.id!!, turma.id!!).andExpect(status().isCreated))
        matricular(segundo.id!!, turma.id!!).andExpect(status().isBadRequest)

        mockMvc.perform(
            patch("/matriculas/$primeiraMatricula/status")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"CANCELADA"}""")
        ).andExpect(status().isOk)

        matricular(segundo.id!!, turma.id!!).andExpect(status().isCreated)
    }

    @Test
    fun menor_de_idade_sem_responsavel_devolve_400() {
        val menor = criarAluno(nome = "Pedrinho", dataNascimento = LocalDate.now().minusYears(9))
        val turma = criarTurma()

        matricular(menor.id!!, turma.id!!).andExpect(status().isBadRequest)

        assertEquals(0, matriculaRepository.count())
        assertEquals(0, contaReceberRepository.count())
    }

    @Test
    fun menor_de_idade_com_responsavel_e_matriculado() {
        val menor = criarAluno(nome = "Pedrinho", dataNascimento = LocalDate.now().minusYears(9))
        criarResponsavel(menor.id!!)
        val turma = criarTurma()

        matricular(menor.id!!, turma.id!!).andExpect(status().isCreated)
    }

    @Test
    fun aluno_inativo_devolve_400() {
        val aluno = criarAluno(ativo = false)
        val turma = criarTurma()

        matricular(aluno.id!!, turma.id!!).andExpect(status().isBadRequest)
    }

    @Test
    fun turma_inativa_devolve_400() {
        val aluno = criarAluno()
        val turma = criarTurma(ativa = false)

        matricular(aluno.id!!, turma.id!!).andExpect(status().isBadRequest)
    }

    @Test
    fun aluno_inexistente_devolve_404() {
        val turma = criarTurma()

        matricular(UUID.randomUUID(), turma.id!!).andExpect(status().isNotFound)
    }

    @Test
    fun turma_inexistente_devolve_404() {
        val aluno = criarAluno()

        matricular(aluno.id!!, UUID.randomUUID()).andExpect(status().isNotFound)
    }

    @Test
    fun dia_de_vencimento_fora_da_faixa_e_barrado_na_validacao_do_payload() {
        val aluno = criarAluno()
        val turma = criarTurma()

        matricular(aluno.id!!, turma.id!!, diaVencimento = 31).andExpect(status().isBadRequest)

        assertEquals(0, matriculaRepository.count())
    }

    // ---------- transições de status ----------

    @Test
    fun ciclo_trancar_e_reativar() {
        val aluno = criarAluno()
        val turma = criarTurma()
        val matriculaId = idDe(matricular(aluno.id!!, turma.id!!).andExpect(status().isCreated))

        fun alterar(status: String) = mockMvc.perform(
            patch("/matriculas/$matriculaId/status")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"$status"}""")
        )

        alterar("TRANCADA").andExpect(status().isOk).andExpect(jsonPath("$.status").value("TRANCADA"))
        alterar("TRANCADA").andExpect(status().isBadRequest)
        alterar("ATIVA").andExpect(status().isOk).andExpect(jsonPath("$.status").value("ATIVA"))
        alterar("CANCELADA").andExpect(status().isOk)
        // CANCELADA é terminal
        alterar("ATIVA").andExpect(status().isBadRequest)

        assertEquals(
            StatusMatriculaType.CANCELADA,
            matriculaRepository.findById(matriculaId).orElseThrow().status
        )
    }

    @Test
    fun a_matricula_trancada_libera_vaga_na_contagem_da_turma() {
        val turma = criarTurma(capacidadeMaxima = 1)
        val primeiro = criarAluno(nome = "Primeira", cpf = "111.111.111-11")
        val segundo = criarAluno(nome = "Segunda", cpf = "222.222.222-22")

        val matriculaId = idDe(matricular(primeiro.id!!, turma.id!!).andExpect(status().isCreated))

        mockMvc.perform(
            patch("/matriculas/$matriculaId/status")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"TRANCADA"}""")
        ).andExpect(status().isOk)

        matricular(segundo.id!!, turma.id!!).andExpect(status().isCreated)
    }

    // ---------- consultas ----------

    @Test
    fun a_turma_expoe_os_alunos_matriculados_e_as_vagas() {
        val turma = criarTurma(capacidadeMaxima = 5)
        val aluno = criarAluno(nome = "Joana")
        matricular(aluno.id!!, turma.id!!).andExpect(status().isCreated)

        mockMvc.perform(get("/turmas/${turma.id}").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.vagasDisponiveis").value(4))

        mockMvc.perform(get("/turmas/${turma.id}/alunos").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].nomeAluno").value("Joana"))
    }

    @Test
    fun a_listagem_sem_filtro_traz_as_matriculas_ativas() {
        val turma = criarTurma()
        val ativa = criarAluno(nome = "Ativa", cpf = "111.111.111-11")
        val cancelada = criarAluno(nome = "Cancelada", cpf = "222.222.222-22")

        matricular(ativa.id!!, turma.id!!).andExpect(status().isCreated)
        val outra = idDe(matricular(cancelada.id!!, turma.id!!).andExpect(status().isCreated))
        mockMvc.perform(
            patch("/matriculas/$outra/status")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"CANCELADA"}""")
        ).andExpect(status().isOk)

        mockMvc.perform(get("/matriculas").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].nomeAluno").value("Ativa"))
    }

    @Test
    fun deve_buscar_matricula_por_id_com_os_dados_do_aluno_e_da_turma() {
        val aluno = criarAluno(nome = "Joana")
        val turma = criarTurma(modalidade = "Jazz")
        val matriculaId = idDe(matricular(aluno.id!!, turma.id!!).andExpect(status().isCreated))

        mockMvc.perform(get("/matriculas/$matriculaId").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nomeAluno").value("Joana"))
            .andExpect(jsonPath("$.modalidadeTurma").value("Jazz"))
    }
}
