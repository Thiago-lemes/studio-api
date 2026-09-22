package com.crative.studio_api.dashboard.service

import com.crative.studio_api.academico.matricula.entity.MatriculaEntity
import com.crative.studio_api.academico.matricula.repository.MatriculaRepository
import com.crative.studio_api.academico.matricula.types.StatusMatriculaType
import com.crative.studio_api.academico.sala.entity.SalaEntity
import com.crative.studio_api.academico.sala.repository.SalaRepository
import com.crative.studio_api.academico.turma.entity.TurmaEntity
import com.crative.studio_api.academico.turma.repository.TurmaRepository
import com.crative.studio_api.academico.turma.types.DiaSemanaType
import com.crative.studio_api.financeiro.exception.PeriodoInvalidoException
import com.crative.studio_api.professor.entity.ProfessorEntity
import com.crative.studio_api.professor.repository.ProfessorRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
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
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgendaServiceTest {

    @Mock lateinit var turmaRepository: TurmaRepository
    @Mock lateinit var professorRepository: ProfessorRepository
    @Mock lateinit var salaRepository: SalaRepository
    @Mock lateinit var matriculaRepository: MatriculaRepository

    private lateinit var service: AgendaService

    private val professorId = UUID.randomUUID()
    private val salaId = UUID.randomUUID()
    private val turmaId = UUID.randomUUID()

    // 2026-09-07 é uma segunda-feira; 2026-09-13, o domingo seguinte.
    private val segunda = LocalDate.of(2026, 9, 7)
    private val domingo = LocalDate.of(2026, 9, 13)

    @BeforeEach
    fun setUp() {
        service = AgendaService(turmaRepository, professorRepository, salaRepository, matriculaRepository)

        given(professorRepository.findAllById(any()))
            .willReturn(listOf(ProfessorEntity(id = professorId, nome = "Marina", telefone = "1", especialidade = null)))
        given(salaRepository.findAllById(any()))
            .willReturn(listOf(SalaEntity(id = salaId, nome = "Sala 1", capacidade = 20)))
        given(matriculaRepository.findAllByTurmaIdIn(any())).willReturn(emptyList())
    }

    private fun turma(
        id: UUID = turmaId,
        dias: Set<DiaSemanaType> = setOf(DiaSemanaType.SEG),
        capacidade: Int = 10,
        professor: UUID = professorId,
        sala: UUID = salaId
    ) = TurmaEntity(
        id = id, modalidade = "Ballet", professorId = professor, salaId = sala,
        diasSemana = dias.toMutableSet(),
        horarioInicio = LocalTime.of(9, 0), horarioFim = LocalTime.of(10, 0),
        capacidadeMaxima = capacidade
    )

    private fun matricula(turma: UUID = turmaId, status: StatusMatriculaType = StatusMatriculaType.ATIVA) =
        MatriculaEntity(
            id = UUID.randomUUID(), alunoId = UUID.randomUUID(), turmaId = turma, status = status,
            valorMensalidade = BigDecimal("300.00"), diaVencimento = 10, dataInicio = LocalDate.now()
        )

    @Test
    fun deve_gerar_uma_ocorrencia_por_dia_da_semana_que_a_turma_acontece() {
        given(turmaRepository.findAllByAtivaTrue())
            .willReturn(listOf(turma(dias = setOf(DiaSemanaType.SEG, DiaSemanaType.QUA))))

        val resultado = service.listar(segunda, domingo, professorId = null, salaId = null)

        assertEquals(2, resultado.size)
        assertEquals(listOf(segunda, segunda.plusDays(2)), resultado.map { it.data })
        assertEquals(listOf(DiaSemanaType.SEG, DiaSemanaType.QUA), resultado.map { it.diaSemana })
    }

    /** O mapeamento DayOfWeek → DiaSemanaType é o ponto mais fácil de errar por um dia. */
    @Test
    fun deve_mapear_domingo_corretamente() {
        given(turmaRepository.findAllByAtivaTrue()).willReturn(listOf(turma(dias = setOf(DiaSemanaType.DOM))))

        val resultado = service.listar(segunda, domingo, professorId = null, salaId = null)

        assertEquals(domingo, resultado.single().data)
    }

    @Test
    fun deve_repetir_a_ocorrencia_a_cada_semana_do_intervalo() {
        given(turmaRepository.findAllByAtivaTrue()).willReturn(listOf(turma(dias = setOf(DiaSemanaType.SEG))))

        val resultado = service.listar(segunda, segunda.plusDays(20), professorId = null, salaId = null)

        assertEquals(3, resultado.size)
    }

    @Test
    fun deve_contar_apenas_matriculas_ativas_nas_vagas() {
        given(turmaRepository.findAllByAtivaTrue()).willReturn(listOf(turma(capacidade = 10)))
        given(matriculaRepository.findAllByTurmaIdIn(any())).willReturn(
            listOf(matricula(), matricula(), matricula(status = StatusMatriculaType.CANCELADA))
        )

        val ocorrencia = service.listar(segunda, domingo, professorId = null, salaId = null).single()

        assertEquals(2, ocorrencia.alunosMatriculados)
        assertEquals(8, ocorrencia.vagasDisponiveis)
    }

    @Test
    fun deve_filtrar_por_professor() {
        val outroProfessor = UUID.randomUUID()
        given(turmaRepository.findAllByAtivaTrue()).willReturn(
            listOf(turma(), turma(id = UUID.randomUUID(), professor = outroProfessor))
        )

        val resultado = service.listar(segunda, domingo, professorId = professorId, salaId = null)

        assertEquals(1, resultado.size)
        assertEquals(professorId, resultado.single().professorId)
    }

    @Test
    fun deve_filtrar_por_sala() {
        given(turmaRepository.findAllByAtivaTrue()).willReturn(
            listOf(turma(), turma(id = UUID.randomUUID(), sala = UUID.randomUUID()))
        )

        assertEquals(1, service.listar(segunda, domingo, professorId = null, salaId = salaId).size)
    }

    @Test
    fun deve_ordenar_por_data_e_horario() {
        val tarde = TurmaEntity(
            id = UUID.randomUUID(), modalidade = "Jazz", professorId = professorId, salaId = salaId,
            diasSemana = mutableSetOf(DiaSemanaType.SEG),
            horarioInicio = LocalTime.of(14, 0), horarioFim = LocalTime.of(15, 0), capacidadeMaxima = 10
        )
        given(turmaRepository.findAllByAtivaTrue()).willReturn(listOf(tarde, turma()))

        val resultado = service.listar(segunda, domingo, professorId = null, salaId = null)

        assertTrue(resultado[0].horarioInicio < resultado[1].horarioInicio)
    }

    @Test
    fun deve_devolver_vazio_quando_nao_ha_turma_ativa() {
        given(turmaRepository.findAllByAtivaTrue()).willReturn(emptyList())

        assertEquals(emptyList<Any>(), service.listar(segunda, domingo, professorId = null, salaId = null))
    }

    @Test
    fun deve_recusar_data_final_anterior_a_inicial() {
        assertThrows(PeriodoInvalidoException::class.java) {
            service.listar(domingo, segunda, professorId = null, salaId = null)
        }
    }

    /** Sem o teto, `?de=2020-01-01&ate=2099-12-31` geraria milhões de linhas. */
    @Test
    fun deve_recusar_periodo_acima_de_366_dias() {
        assertThrows(PeriodoInvalidoException::class.java) {
            service.listar(segunda, segunda.plusDays(366), professorId = null, salaId = null)
        }
    }

    @Test
    fun deve_aceitar_periodo_de_exatamente_366_dias() {
        given(turmaRepository.findAllByAtivaTrue()).willReturn(emptyList())

        service.listar(segunda, segunda.plusDays(365), professorId = null, salaId = null)
    }
}
