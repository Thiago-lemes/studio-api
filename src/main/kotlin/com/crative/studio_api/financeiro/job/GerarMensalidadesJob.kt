package com.crative.studio_api.financeiro.job

import com.crative.studio_api.academico.matricula.repository.MatriculaRepository
import com.crative.studio_api.academico.matricula.types.StatusMatriculaType
import com.crative.studio_api.financeiro.service.ContaReceberService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.YearMonth

/**
 * Gera a cobrança do mês corrente pra cada matrícula ATIVA.
 *
 * Roda todo dia 1 às 03:00. Idempotente via `UNIQUE(matricula_id, referencia)` —
 * rodar de novo no mesmo mês não duplica cobrança.
 */
@Component
class GerarMensalidadesJob(
    private val matriculaRepository: MatriculaRepository,
    private val contaReceberService: ContaReceberService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 3 1 * *")
    fun executar() {
        gerarPara(YearMonth.now())
    }

    fun gerarPara(referencia: YearMonth): Int {
        val matriculasAtivas = matriculaRepository.findAllByStatus(StatusMatriculaType.ATIVA)
        log.info("Gerando mensalidades de {} para {} matrícula(s) ativa(s)", referencia, matriculasAtivas.size)

        var geradas = 0
        matriculasAtivas.forEach { matricula ->
            runCatching { contaReceberService.gerarCobrancaMensal(matricula, referencia) }
                .onSuccess { geradas++ }
                .onFailure { erro ->
                    // uma matrícula problemática não pode abortar a geração das demais
                    log.error("Falha ao gerar cobrança da matrícula {}: {}", matricula.id, erro.message)
                }
        }

        log.info("Mensalidades de {} processadas: {} de {}", referencia, geradas, matriculasAtivas.size)
        return geradas
    }
}
