package com.crative.studio_api.dashboard.service

import com.crative.studio_api.academico.matricula.entity.MatriculaEntity
import com.crative.studio_api.academico.matricula.repository.MatriculaRepository
import com.crative.studio_api.academico.matricula.types.StatusMatriculaType
import com.crative.studio_api.academico.turma.entity.TurmaEntity
import com.crative.studio_api.academico.turma.repository.TurmaRepository
import com.crative.studio_api.academico.turma.types.DiaSemanaType
import com.crative.studio_api.aluno.repository.AlunoRepository
import com.crative.studio_api.financeiro.dto.response.DashboardFinanceiroResponse
import com.crative.studio_api.financeiro.service.FinanceiroDashboardService
import com.crative.studio_api.professor.repository.ProfessorRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.quality.Strictness
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardGeralServiceTest {

    @Mock lateinit var alunoRepository: AlunoRepository
    @Mock lateinit var professorRepository: ProfessorRepository
    @Mock lateinit var turmaRepository: TurmaRepository
    @Mock lateinit var matriculaRepository: MatriculaRepository
    @Mock lateinit var financeiroDashboardService: FinanceiroDashboardService

    private lateinit var service: DashboardGeralService

    private val referencia = YearMonth.of(2026, 9)

    @BeforeEach
    fun setUp() {
        service = DashboardGeralService(
            alunoRepository, professorRepository, turmaRepository, matriculaRepository, financeiroDashboardService
        )

        given(alunoRepository.countByAtivoTrue()).willReturn(0)
        given(professorRepository.countByAtivoTrue()).willReturn(0)
        given(turmaRepository.findAllByAtivaTrue()).willReturn(emptyList())
        given(matriculaRepository.countByStatus(StatusMatriculaType.ATIVA)).willReturn(0)
        given(matriculaRepository.findAllByTurmaIdIn(any())).willReturn(emptyList())
        given(financeiroDashboardService.montar(any())).willReturn(
            DashboardFinanceiroResponse(
                referencia = "2026-09",
                totalReceberMes = BigDecimal.ZERO, totalRecebidoMes = BigDecimal.ZERO,
                totalAtrasado = BigDecimal.ZERO, quantidadeAlunosInadimplentes = 0,
                totalPagarMes = BigDecimal.ZERO, saldoProjetadoMes = BigDecimal.ZERO
            )
        )
    }

    private fun turma(id: UUID = UUID.randomUUID(), capacidade: Int) = TurmaEntity(
        id = id, modalidade = "Ballet", professorId = UUID.randomUUID(), salaId = UUID.randomUUID(),
        diasSemana = mutableSetOf(DiaSemanaType.SEG),
        horarioInicio = LocalTime.of(9, 0), horarioFim = LocalTime.of(10, 0), capacidadeMaxima = capacidade
    )

    private fun matricula(turmaId: UUID, status: StatusMatriculaType = StatusMatriculaType.ATIVA) =
        MatriculaEntity(
            id = UUID.randomUUID(), alunoId = UUID.randomUUID(), turmaId = turmaId, status = status,
            valorMensalidade = BigDecimal("300.00"), diaVencimento = 10, dataInicio = LocalDate.now()
        )

    @Test
    fun deve_somar_os_contadores_academicos() {
        given(alunoRepository.countByAtivoTrue()).willReturn(42)
        given(professorRepository.countByAtivoTrue()).willReturn(5)
        given(matriculaRepository.countByStatus(StatusMatriculaType.ATIVA)).willReturn(60)
        given(turmaRepository.findAllByAtivaTrue()).willReturn(listOf(turma(capacidade = 10), turma(capacidade = 20)))

        val resultado = service.montar(referencia).academico

        assertEquals(42, resultado.alunosAtivos)
        assertEquals(5, resultado.professoresAtivos)
        assertEquals(2, resultado.turmasAtivas)
        assertEquals(60, resultado.matriculasAtivas)
        assertEquals(30, resultado.capacidadeTotal)
    }

    /**
     * O ponto do cálculo por turma: uma turma lotada não empresta vaga para a vazia. Pelo total
     * agregado (30 − 10) o estúdio pareceria ter 20 vagas; na prática só a segunda turma as tem.
     */
    @Test
    fun vagas_disponiveis_sao_por_turma_e_nao_pelo_total_agregado() {
        val lotada = turma(capacidade = 5)
        val vazia = turma(capacidade = 25)
        given(turmaRepository.findAllByAtivaTrue()).willReturn(listOf(lotada, vazia))
        given(matriculaRepository.findAllByTurmaIdIn(any()))
            .willReturn(List(5) { matricula(lotada.id!!) })

        assertEquals(25, service.montar(referencia).academico.vagasDisponiveis)
    }

    /** Turma que teve a capacidade reduzida depois de cheia não pode puxar o total para baixo. */
    @Test
    fun turma_acima_da_capacidade_nao_gera_vaga_negativa() {
        val estourada = turma(capacidade = 2)
        val outra = turma(capacidade = 10)
        given(turmaRepository.findAllByAtivaTrue()).willReturn(listOf(estourada, outra))
        given(matriculaRepository.findAllByTurmaIdIn(any()))
            .willReturn(List(6) { matricula(estourada.id!!) })

        assertEquals(10, service.montar(referencia).academico.vagasDisponiveis)
    }

    @Test
    fun matricula_cancelada_nao_ocupa_vaga() {
        val turma = turma(capacidade = 10)
        given(turmaRepository.findAllByAtivaTrue()).willReturn(listOf(turma))
        given(matriculaRepository.findAllByTurmaIdIn(any())).willReturn(
            listOf(matricula(turma.id!!), matricula(turma.id!!, StatusMatriculaType.CANCELADA))
        )

        assertEquals(9, service.montar(referencia).academico.vagasDisponiveis)
    }

    /** O bloco financeiro é delegado, não recalculado — é o que impede os dois números de divergirem. */
    @Test
    fun deve_delegar_o_bloco_financeiro_ao_dashboard_do_mes() {
        val resultado = service.montar(referencia)

        assertEquals("2026-09", resultado.financeiro.referencia)
        org.mockito.kotlin.verify(financeiroDashboardService).montar(referencia)
    }

    @Test
    fun estudio_sem_turma_ativa_devolve_zeros() {
        val resultado = service.montar(referencia).academico

        assertEquals(0, resultado.capacidadeTotal)
        assertEquals(0, resultado.vagasDisponiveis)
    }
}
