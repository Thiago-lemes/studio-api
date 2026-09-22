package com.crative.studio_api.academico.matricula

import com.crative.studio_api.academico.matricula.entity.MatriculaEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class CalculoMensalidadeTest {

    private fun matricula(valor: String, desconto: String) = MatriculaEntity(
        alunoId = UUID.randomUUID(),
        turmaId = UUID.randomUUID(),
        valorMensalidade = BigDecimal(valor),
        diaVencimento = 10,
        descontoPercentual = BigDecimal(desconto),
        dataInicio = LocalDate.now()
    )

    @Test
    fun sem_desconto_o_valor_efetivo_e_a_mensalidade_com_duas_casas() {
        assertEquals(BigDecimal("300.00"), matricula("300.00", "0").valorEfetivo())
    }

    @Test
    fun aplica_desconto_percentual() {
        assertEquals(BigDecimal("270.00"), matricula("300.00", "10").valorEfetivo())
    }

    @Test
    fun desconto_de_cem_por_cento_zera_a_mensalidade() {
        assertEquals(BigDecimal("0.00"), matricula("300.00", "100").valorEfetivo())
    }

    @Test
    fun arredonda_para_duas_casas_com_half_up() {
        // 199.90 * (1 - 0.0750) = 184.9075 → 184.91
        assertEquals(BigDecimal("184.91"), matricula("199.90", "7.5").valorEfetivo())
    }

    @Test
    fun desconto_com_dizima_usa_quatro_casas_no_fator_antes_de_arredondar() {
        // 33.33% → fator 0.6667; 300 * 0.6667 = 200.01
        assertEquals(BigDecimal("200.01"), matricula("300.00", "33.33").valorEfetivo())
    }

    @Test
    fun preserva_duas_casas_mesmo_quando_a_mensalidade_vem_sem_escala() {
        assertEquals(BigDecimal("250.00"), matricula("250", "0").valorEfetivo())
    }
}
