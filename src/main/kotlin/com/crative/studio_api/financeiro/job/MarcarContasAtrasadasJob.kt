package com.crative.studio_api.financeiro.job

import com.crative.studio_api.financeiro.service.ContaReceberService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Marca como ATRASADO toda conta a receber PENDENTE cujo vencimento já passou.
 * Roda diariamente às 02:00.
 *
 * O disparo de aviso ao responsável (seção de Comunicação do design) entra aqui quando
 * aquele módulo existir — hoje o job só atualiza o status.
 */
@Component
class MarcarContasAtrasadasJob(
    private val contaReceberService: ContaReceberService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 2 * * *")
    fun executar(): Int {
        val atualizadas = contaReceberService.marcarContasAtrasadas()
        log.info("Contas a receber marcadas como ATRASADO: {}", atualizadas.size)
        return atualizadas.size
    }
}
