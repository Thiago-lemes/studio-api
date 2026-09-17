package com.crative.studio_api.financeiro.service

import com.crative.studio_api.academico.matricula.repository.MatriculaRepository
import com.crative.studio_api.financeiro.dto.response.DashboardFinanceiroResponse
import com.crative.studio_api.financeiro.repository.ContaPagarRepository
import com.crative.studio_api.financeiro.repository.ContaReceberRepository
import com.crative.studio_api.financeiro.repository.PagamentoRepository
import com.crative.studio_api.financeiro.types.StatusContaType
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalTime
import java.time.YearMonth

/**
 * Leitura agregada do mês — sem escrita. Definições usadas:
 * - `totalReceberMes`: tudo que vence no mês (PENDENTE + ATRASADO + PAGO), ou seja, o previsto
 * - `totalRecebidoMes`: soma dos pagamentos efetivamente registrados no mês (por `pagoEm`,
 *   não por competência — um pagamento atrasado entra no mês em que o dinheiro entrou)
 * - `totalAtrasado`: todas as contas ATRASADO, de qualquer competência, não só do mês
 * - `saldoProjetadoMes`: previsto a receber − previsto a pagar no mês
 */
@Service
class FinanceiroDashboardService(
    private val contaReceberRepository: ContaReceberRepository,
    private val contaPagarRepository: ContaPagarRepository,
    private val pagamentoRepository: PagamentoRepository,
    private val matriculaRepository: MatriculaRepository
) {

    fun montar(referencia: YearMonth): DashboardFinanceiroResponse {
        val primeiroDia = referencia.atDay(1)
        val ultimoDia = referencia.atEndOfMonth()

        val totalReceberMes = contaReceberRepository
            .findAllByVencimentoBetween(primeiroDia, ultimoDia)
            .somar { it.valor }

        val totalRecebidoMes = pagamentoRepository
            .findAllByPagoEmBetween(primeiroDia.atStartOfDay(), ultimoDia.atTime(LocalTime.MAX))
            .somar { it.valorPago }

        val contasAtrasadas = contaReceberRepository.findAllByStatus(StatusContaType.ATRASADO)
        val totalAtrasado = contasAtrasadas.somar { it.valor }

        val quantidadeAlunosInadimplentes = contasAtrasadas
            .mapNotNull { conta -> matriculaRepository.findById(conta.matriculaId).orElse(null)?.alunoId }
            .distinct()
            .size

        val totalPagarMes = contaPagarRepository
            .findAllByVencimentoBetween(primeiroDia, ultimoDia)
            .somar { it.valor }

        return DashboardFinanceiroResponse(
            referencia = referencia.toString(),
            totalReceberMes = totalReceberMes,
            totalRecebidoMes = totalRecebidoMes,
            totalAtrasado = totalAtrasado,
            quantidadeAlunosInadimplentes = quantidadeAlunosInadimplentes,
            totalPagarMes = totalPagarMes,
            saldoProjetadoMes = totalReceberMes - totalPagarMes
        )
    }

    private fun <T> List<T>.somar(seletor: (T) -> BigDecimal): BigDecimal =
        fold(BigDecimal.ZERO) { acumulado, item -> acumulado + seletor(item) }
}
