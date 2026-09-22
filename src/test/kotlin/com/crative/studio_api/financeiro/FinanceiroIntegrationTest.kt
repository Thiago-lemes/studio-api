package com.crative.studio_api.financeiro

import com.crative.studio_api.financeiro.entity.ContaPagarEntity
import com.crative.studio_api.financeiro.entity.ContaReceberEntity
import com.crative.studio_api.financeiro.entity.PagamentoEntity
import com.crative.studio_api.financeiro.types.CategoriaDespesaType
import com.crative.studio_api.financeiro.types.FormaPagamentoType
import com.crative.studio_api.financeiro.types.StatusContaType
import com.crative.studio_api.shared.AbstractIntegrationTest
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class FinanceiroIntegrationTest : AbstractIntegrationTest() {

    private val auth by lazy { tokenDe() }

    // ---------- contas a pagar ----------

    private fun registrarContaPagar(
        descricao: String = "Aluguel",
        categoria: CategoriaDespesaType = CategoriaDespesaType.ALUGUEL,
        valor: String = "2500.00",
        vencimento: LocalDate = LocalDate.now().plusDays(10)
    ) = mockMvc.perform(
        post("/contas-pagar")
            .header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"descricao":"$descricao","categoria":"$categoria","valor":$valor,"vencimento":"$vencimento"}
                """.trimIndent()
            )
    )

    @Test
    fun deve_registrar_conta_a_pagar_pendente() {
        registrarContaPagar(vencimento = LocalDate.now().plusDays(10))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("PENDENTE"))
            .andExpect(jsonPath("$.descricao").value("Aluguel"))

        assertEquals(1, contaPagarRepository.count())
    }

    @Test
    fun conta_a_pagar_com_vencimento_passado_nasce_atrasada() {
        registrarContaPagar(vencimento = LocalDate.now().minusDays(3))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("ATRASADO"))
    }

    @Test
    fun valor_nao_positivo_na_conta_a_pagar_devolve_400() {
        registrarContaPagar(valor = "0.00").andExpect(status().isBadRequest)

        assertEquals(0, contaPagarRepository.count())
    }

    @Test
    fun descricao_em_branco_na_conta_a_pagar_devolve_400() {
        registrarContaPagar(descricao = "   ").andExpect(status().isBadRequest)
    }

    @Test
    fun deve_quitar_conta_a_pagar_gravando_a_data() {
        val resultado = registrarContaPagar().andExpect(status().isCreated)
        val id = UUID.fromString(JsonPath.read(resultado.andReturn().response.contentAsString, "$.id"))
        val momento = LocalDateTime.of(2026, 4, 10, 14, 30)

        mockMvc.perform(
            patch("/contas-pagar/$id/pagar")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"dataPagamento":"$momento"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PAGO"))

        val naBase = contaPagarRepository.findById(id).orElseThrow()
        assertEquals(StatusContaType.PAGO, naBase.status)
        assertNotNull(naBase.pagoEm)
    }

    @Test
    fun quitar_conta_a_pagar_duas_vezes_devolve_409() {
        val resultado = registrarContaPagar().andExpect(status().isCreated)
        val id = JsonPath.read<String>(resultado.andReturn().response.contentAsString, "$.id")
        val corpo = """{"dataPagamento":"${LocalDateTime.now()}"}"""

        mockMvc.perform(
            patch("/contas-pagar/$id/pagar").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(corpo)
        ).andExpect(status().isOk)

        mockMvc.perform(
            patch("/contas-pagar/$id/pagar").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(corpo)
        ).andExpect(status().isConflict)
    }

    @Test
    fun quitar_conta_a_pagar_inexistente_devolve_404() {
        mockMvc.perform(
            patch("/contas-pagar/${UUID.randomUUID()}/pagar")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"dataPagamento":"${LocalDateTime.now()}"}""")
        ).andExpect(status().isNotFound)
    }

    @Test
    fun deve_filtrar_contas_a_pagar_por_status() {
        registrarContaPagar(descricao = "Futura", vencimento = LocalDate.now().plusDays(5))
        registrarContaPagar(descricao = "Vencida", vencimento = LocalDate.now().minusDays(5))

        mockMvc.perform(get("/contas-pagar").param("status", "ATRASADO").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].descricao").value("Vencida"))

        mockMvc.perform(get("/contas-pagar").header("Authorization", auth))
            .andExpect(jsonPath("$.length()").value(2))
    }

    // ---------- dashboard ----------

    private fun matriculaCom(nome: String, cpf: String, turmaId: UUID) = criarMatricula(
        alunoId = criarAluno(nome = nome, cpf = cpf).id!!,
        turmaId = turmaId
    )

    private fun contaReceber(
        matriculaId: UUID,
        referencia: String,
        valor: String,
        vencimento: LocalDate,
        status: StatusContaType = StatusContaType.PENDENTE
    ) = contaReceberRepository.save(
        ContaReceberEntity(
            matriculaId = matriculaId, referencia = referencia, valor = BigDecimal(valor),
            vencimento = vencimento, status = status
        )
    )

    @Test
    fun dashboard_sem_movimento_vem_zerado() {
        mockMvc.perform(get("/financeiro/dashboard").param("referencia", "2026-04").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.referencia").value("2026-04"))
            .andExpect(jsonPath("$.totalReceberMes").value(0))
            .andExpect(jsonPath("$.totalRecebidoMes").value(0))
            .andExpect(jsonPath("$.totalAtrasado").value(0))
            .andExpect(jsonPath("$.quantidadeAlunosInadimplentes").value(0))
            .andExpect(jsonPath("$.saldoProjetadoMes").value(0))
    }

    @Test
    fun dashboard_consolida_previsto_recebido_atrasado_e_saldo() {
        val turma = criarTurma(capacidadeMaxima = 10)
        val matricula = matriculaCom("Joana", "111.111.111-11", turma.id!!)
        // segunda matrícula porque conta_receber tem UNIQUE(matricula_id, referencia)
        val outra = matriculaCom("Pedro", "222.222.222-22", turma.id!!)

        // previsto do mês: 300 pendente + 200 já paga = 500
        contaReceber(matricula.id!!, "2026-04", "300.00", LocalDate.of(2026, 4, 10))
        val paga = contaReceber(
            outra.id!!, "2026-04", "200.00", LocalDate.of(2026, 4, 20), StatusContaType.PAGO
        )
        // caixa do mês: o pagamento daquela conta
        pagamentoRepository.save(
            PagamentoEntity(
                contaReceberId = paga.id!!, valorPago = BigDecimal("200.00"),
                formaPagamento = FormaPagamentoType.PIX, pagoEm = LocalDateTime.of(2026, 4, 20, 9, 0)
            )
        )
        // atrasado de competência anterior — entra no totalAtrasado, não no previsto do mês
        contaReceber(matricula.id!!, "2026-02", "150.00", LocalDate.of(2026, 2, 10), StatusContaType.ATRASADO)
        // despesa do mês
        contaPagarRepository.save(
            ContaPagarEntity(
                descricao = "Aluguel", categoria = CategoriaDespesaType.ALUGUEL,
                valor = BigDecimal("400.00"), vencimento = LocalDate.of(2026, 4, 5)
            )
        )

        mockMvc.perform(get("/financeiro/dashboard").param("referencia", "2026-04").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalReceberMes").value(500.00))
            .andExpect(jsonPath("$.totalRecebidoMes").value(200.00))
            .andExpect(jsonPath("$.totalAtrasado").value(150.00))
            .andExpect(jsonPath("$.quantidadeAlunosInadimplentes").value(1))
            .andExpect(jsonPath("$.totalPagarMes").value(400.00))
            .andExpect(jsonPath("$.saldoProjetadoMes").value(100.00))
    }

    @Test
    fun alunos_inadimplentes_sao_contados_por_aluno_nao_por_conta() {
        val turma = criarTurma(capacidadeMaxima = 10)
        val joana = matriculaCom("Joana", "111.111.111-11", turma.id!!)
        val pedro = matriculaCom("Pedro", "222.222.222-22", turma.id!!)

        // Joana com três contas atrasadas, Pedro com uma: dois inadimplentes
        listOf("2026-01", "2026-02", "2026-03").forEach {
            contaReceber(joana.id!!, it, "300.00", LocalDate.of(2026, 1, 10), StatusContaType.ATRASADO)
        }
        contaReceber(pedro.id!!, "2026-03", "300.00", LocalDate.of(2026, 3, 10), StatusContaType.ATRASADO)

        mockMvc.perform(get("/financeiro/dashboard").param("referencia", "2026-04").header("Authorization", auth))
            .andExpect(jsonPath("$.quantidadeAlunosInadimplentes").value(2))
            .andExpect(jsonPath("$.totalAtrasado").value(1200.00))
    }

    @Test
    fun dashboard_sem_referencia_usa_o_mes_corrente() {
        mockMvc.perform(get("/financeiro/dashboard").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.referencia").value(YearMonth.now().toString()))
    }

    @Test
    fun referencia_fora_do_formato_devolve_400() {
        mockMvc.perform(get("/financeiro/dashboard").param("referencia", "abril/2026").header("Authorization", auth))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun saldo_projetado_negativo_e_reportado() {
        contaPagarRepository.save(
            ContaPagarEntity(
                descricao = "Reforma", categoria = CategoriaDespesaType.OUTROS,
                valor = BigDecimal("900.00"), vencimento = LocalDate.of(2026, 4, 5)
            )
        )

        mockMvc.perform(get("/financeiro/dashboard").param("referencia", "2026-04").header("Authorization", auth))
            .andExpect(jsonPath("$.saldoProjetadoMes").value(-900.00))
    }

    // ---------- contas a receber ----------

    @Test
    fun conta_a_receber_atrasada_informa_os_dias_de_atraso() {
        val turma = criarTurma()
        val matricula = matriculaCom("Joana", "111.111.111-11", turma.id!!)
        val conta = contaReceber(
            matricula.id!!, "2026-01", "300.00", LocalDate.now().minusDays(9), StatusContaType.ATRASADO
        )

        mockMvc.perform(get("/contas-receber/${conta.id}").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.diasEmAtraso").value(9))
            .andExpect(jsonPath("$.nomeAluno").value("Joana"))
    }

    @Test
    fun conta_a_receber_pendente_nao_tem_dias_de_atraso() {
        val turma = criarTurma()
        val matricula = matriculaCom("Joana", "111.111.111-11", turma.id!!)
        val conta = contaReceber(matricula.id!!, "2026-05", "300.00", LocalDate.now().plusDays(5))

        mockMvc.perform(get("/contas-receber/${conta.id}").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.diasEmAtraso").doesNotExist())
    }

    @Test
    fun buscar_conta_a_receber_inexistente_devolve_404() {
        mockMvc.perform(get("/contas-receber/${UUID.randomUUID()}").header("Authorization", auth))
            .andExpect(status().isNotFound)
    }

    @Test
    fun deve_filtrar_contas_a_receber_por_status() {
        val turma = criarTurma()
        val matricula = matriculaCom("Joana", "111.111.111-11", turma.id!!)
        contaReceber(matricula.id!!, "2026-01", "300.00", LocalDate.now().minusDays(20), StatusContaType.ATRASADO)
        contaReceber(matricula.id!!, "2026-05", "300.00", LocalDate.now().plusDays(5))

        mockMvc.perform(get("/contas-receber").param("status", "ATRASADO").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].referencia").value("2026-01"))
    }
}
