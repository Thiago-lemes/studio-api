package com.crative.studio_api.financeiro.job

import com.crative.studio_api.financeiro.entity.ContaReceberEntity
import com.crative.studio_api.financeiro.service.ContaReceberService
import com.crative.studio_api.financeiro.types.StatusContaType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class MarcarContasAtrasadasJobTest {

    @Mock
    lateinit var contaReceberService: ContaReceberService

    private lateinit var job: MarcarContasAtrasadasJob

    @BeforeEach
    fun setUp() {
        job = MarcarContasAtrasadasJob(contaReceberService)
    }

    private fun conta() = ContaReceberEntity(
        id = UUID.randomUUID(), matriculaId = UUID.randomUUID(), referencia = "2026-03",
        valor = BigDecimal("300.00"), vencimento = LocalDate.now().minusDays(5),
        status = StatusContaType.ATRASADO
    )

    @Test
    fun devolve_a_quantidade_de_contas_atualizadas() {
        given(contaReceberService.marcarContasAtrasadas()).willReturn(listOf(conta(), conta(), conta()))

        assertEquals(3, job.executar())
    }

    @Test
    fun devolve_zero_quando_nao_ha_conta_vencida() {
        given(contaReceberService.marcarContasAtrasadas()).willReturn(emptyList())

        assertEquals(0, job.executar())
        verify(contaReceberService).marcarContasAtrasadas()
    }

    @Test
    fun a_falha_do_servico_e_propagada_para_o_agendador() {
        given(contaReceberService.marcarContasAtrasadas()).willThrow(IllegalStateException("banco fora"))

        assertThrows(IllegalStateException::class.java) { job.executar() }
    }
}
