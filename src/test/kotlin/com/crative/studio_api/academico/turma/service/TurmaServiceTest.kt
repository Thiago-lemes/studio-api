package com.crative.studio_api.academico.turma.service

import com.crative.studio_api.academico.exception.ChoqueDeHorarioException
import com.crative.studio_api.academico.exception.SalaNaoEncontradaException
import com.crative.studio_api.academico.exception.TurmaComMatriculasAtivasException
import com.crative.studio_api.academico.exception.TurmaNaoEncontradaException
import com.crative.studio_api.academico.matricula.entity.MatriculaEntity
import com.crative.studio_api.academico.matricula.repository.MatriculaRepository
import com.crative.studio_api.academico.matricula.types.StatusMatriculaType
import com.crative.studio_api.academico.sala.entity.SalaEntity
import com.crative.studio_api.academico.sala.repository.SalaRepository
import com.crative.studio_api.academico.turma.entity.TurmaEntity
import com.crative.studio_api.academico.turma.repository.TurmaRepository
import com.crative.studio_api.academico.turma.types.DiaSemanaType.QUA
import com.crative.studio_api.academico.turma.types.DiaSemanaType.SEG
import com.crative.studio_api.academico.turma.types.DiaSemanaType.SEX
import com.crative.studio_api.academico.turma.types.DiaSemanaType.TER
import com.crative.studio_api.aluno.entity.AlunoEntity
import com.crative.studio_api.aluno.exception.AlunoNaoEncontradoException
import com.crative.studio_api.aluno.repository.AlunoRepository
import com.crative.studio_api.professor.entity.ProfessorEntity
import com.crative.studio_api.professor.exception.ProfessorNaoEncontradoException
import com.crative.studio_api.professor.repository.ProfessorRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
import org.mockito.kotlin.check
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.quality.Strictness
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TurmaServiceTest {

    @Mock lateinit var repository: TurmaRepository
    @Mock lateinit var professorRepository: ProfessorRepository
    @Mock lateinit var salaRepository: SalaRepository
    @Mock lateinit var matriculaRepository: MatriculaRepository
    @Mock lateinit var alunoRepository: AlunoRepository

    private lateinit var service: TurmaService

    private val professorId = UUID.randomUUID()
    private val salaId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service = TurmaService(repository, professorRepository, salaRepository, matriculaRepository, alunoRepository)

        given(professorRepository.existsById(professorId)).willReturn(true)
        given(salaRepository.existsById(salaId)).willReturn(true)
        given(professorRepository.findById(professorId))
            .willReturn(Optional.of(ProfessorEntity(id = professorId, nome = "Marina", telefone = "1", especialidade = null)))
        given(salaRepository.findById(salaId))
            .willReturn(Optional.of(SalaEntity(id = salaId, nome = "Sala 1", capacidade = 20)))
        // o save do JPA atribui o id; sem isso o detalhar() da turma nova estouraria
        given(repository.save(any<TurmaEntity>())).willAnswer { invocacao ->
            val salva = invocacao.arguments[0] as TurmaEntity
            if (salva.id != null) salva else comId(salva)
        }
    }

    private fun comId(t: TurmaEntity) = TurmaEntity(
        id = UUID.randomUUID(),
        modalidade = t.modalidade,
        professorId = t.professorId,
        salaId = t.salaId,
        diasSemana = t.diasSemana,
        horarioInicio = t.horarioInicio,
        horarioFim = t.horarioFim,
        capacidadeMaxima = t.capacidadeMaxima,
        ativa = t.ativa
    )

    private fun turma(
        id: UUID? = UUID.randomUUID(),
        dias: Set<com.crative.studio_api.academico.turma.types.DiaSemanaType> = setOf(SEG),
        inicio: LocalTime = LocalTime.of(9, 0),
        fim: LocalTime = LocalTime.of(10, 0),
        capacidade: Int = 10,
        professor: UUID = professorId,
        sala: UUID = salaId
    ) = TurmaEntity(
        id = id,
        modalidade = "Ballet",
        professorId = professor,
        salaId = sala,
        diasSemana = dias.toMutableSet(),
        horarioInicio = inicio,
        horarioFim = fim,
        capacidadeMaxima = capacidade
    )

    private fun criarPadrao(
        dias: Set<com.crative.studio_api.academico.turma.types.DiaSemanaType> = setOf(SEG),
        inicio: LocalTime = LocalTime.of(9, 0),
        fim: LocalTime = LocalTime.of(10, 0),
        capacidade: Int = 10
    ) = service.criar("Ballet", professorId, salaId, dias, inicio, fim, capacidade)

    // ---------- criação e detalhamento ----------

    @Test
    fun deve_criar_turma_detalhando_professor_sala_e_vagas() {
        given(repository.findAllBySalaIdAndAtivaTrue(salaId)).willReturn(emptyList())
        given(repository.findAllByProfessorIdAndAtivaTrue(professorId)).willReturn(emptyList())
        given(matriculaRepository.countByTurmaIdAndStatus(any(), any())).willReturn(0)

        val resultado = criarPadrao(capacidade = 10)

        assertEquals("Marina", resultado.professorNome)
        assertEquals("Sala 1", resultado.salaNome)
        assertEquals(10, resultado.vagasDisponiveis)
    }

    @Test
    fun vagas_disponiveis_descontam_as_matriculas_ativas() {
        given(repository.findAllBySalaIdAndAtivaTrue(salaId)).willReturn(emptyList())
        given(repository.findAllByProfessorIdAndAtivaTrue(professorId)).willReturn(emptyList())
        given(matriculaRepository.countByTurmaIdAndStatus(any(), any())).willReturn(4)

        assertEquals(6, criarPadrao(capacidade = 10).vagasDisponiveis)
    }

    @Test
    fun deve_falhar_quando_o_professor_nao_existe_sem_chegar_a_gravar() {
        val outroProfessor = UUID.randomUUID()
        given(professorRepository.existsById(outroProfessor)).willReturn(false)

        assertThrows(ProfessorNaoEncontradoException::class.java) {
            service.criar("Ballet", outroProfessor, salaId, setOf(SEG), LocalTime.of(9, 0), LocalTime.of(10, 0), 10)
        }
        verify(repository, never()).save(any<TurmaEntity>())
    }

    @Test
    fun deve_falhar_quando_a_sala_nao_existe_sem_chegar_a_gravar() {
        val outraSala = UUID.randomUUID()
        given(salaRepository.existsById(outraSala)).willReturn(false)

        assertThrows(SalaNaoEncontradaException::class.java) {
            service.criar("Ballet", professorId, outraSala, setOf(SEG), LocalTime.of(9, 0), LocalTime.of(10, 0), 10)
        }
        verify(repository, never()).save(any<TurmaEntity>())
    }

    @Test
    fun os_vinculos_sao_validados_antes_do_choque_de_horario() {
        val outroProfessor = UUID.randomUUID()
        given(professorRepository.existsById(outroProfessor)).willReturn(false)
        // a sala está ocupada, mas o professor inexistente é o erro que vem primeiro
        given(repository.findAllBySalaIdAndAtivaTrue(salaId)).willReturn(listOf(turma()))

        assertThrows(ProfessorNaoEncontradoException::class.java) {
            service.criar("Ballet", outroProfessor, salaId, setOf(SEG), LocalTime.of(9, 0), LocalTime.of(10, 0), 10)
        }
    }

    @Test
    fun atualizar_para_sala_inexistente_falha_sem_alterar_a_turma() {
        val id = UUID.randomUUID()
        val outraSala = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(turma(id = id)))
        given(salaRepository.existsById(outraSala)).willReturn(false)

        assertThrows(SalaNaoEncontradaException::class.java) {
            service.atualizar(id, professorId, outraSala, setOf(SEG), LocalTime.of(9, 0), LocalTime.of(10, 0), 10)
        }
        verify(repository, never()).save(any<TurmaEntity>())
    }

    // ---------- choque de horário na sala ----------

    @Test
    fun deve_recusar_turma_que_sobrepoe_horario_na_mesma_sala_e_dia() {
        given(repository.findAllBySalaIdAndAtivaTrue(salaId))
            .willReturn(listOf(turma(dias = setOf(SEG), inicio = LocalTime.of(9, 0), fim = LocalTime.of(10, 0))))

        val erro = assertThrows(ChoqueDeHorarioException::class.java) {
            criarPadrao(dias = setOf(SEG), inicio = LocalTime.of(9, 30), fim = LocalTime.of(10, 30))
        }
        assertEquals("Já existe uma turma nessa sala, nesse dia e horário", erro.message)
        verify(repository, never()).save(any<TurmaEntity>())
    }

    @Test
    fun turmas_encostadas_na_mesma_sala_nao_conflitam() {
        // 09:00–10:00 e 10:00–11:00: o fim de uma é o início da outra, o intervalo é aberto
        given(repository.findAllBySalaIdAndAtivaTrue(salaId))
            .willReturn(listOf(turma(dias = setOf(SEG), inicio = LocalTime.of(9, 0), fim = LocalTime.of(10, 0))))
        given(repository.findAllByProfessorIdAndAtivaTrue(professorId)).willReturn(emptyList())
        given(matriculaRepository.countByTurmaIdAndStatus(any(), any())).willReturn(0)

        criarPadrao(dias = setOf(SEG), inicio = LocalTime.of(10, 0), fim = LocalTime.of(11, 0))

        verify(repository).save(any<TurmaEntity>())
    }

    @Test
    fun mesmo_horario_em_dias_diferentes_na_mesma_sala_nao_conflita() {
        given(repository.findAllBySalaIdAndAtivaTrue(salaId))
            .willReturn(listOf(turma(dias = setOf(SEG))))
        given(repository.findAllByProfessorIdAndAtivaTrue(professorId)).willReturn(emptyList())
        given(matriculaRepository.countByTurmaIdAndStatus(any(), any())).willReturn(0)

        criarPadrao(dias = setOf(TER))

        verify(repository).save(any<TurmaEntity>())
    }

    @Test
    fun conflito_basta_um_dia_em_comum_entre_os_conjuntos() {
        given(repository.findAllBySalaIdAndAtivaTrue(salaId))
            .willReturn(listOf(turma(dias = setOf(SEG, QUA))))

        assertThrows(ChoqueDeHorarioException::class.java) {
            criarPadrao(dias = setOf(QUA, SEX))
        }
    }

    @Test
    fun turma_que_engloba_completamente_a_existente_conflita() {
        given(repository.findAllBySalaIdAndAtivaTrue(salaId))
            .willReturn(listOf(turma(dias = setOf(SEG), inicio = LocalTime.of(9, 0), fim = LocalTime.of(10, 0))))

        assertThrows(ChoqueDeHorarioException::class.java) {
            criarPadrao(dias = setOf(SEG), inicio = LocalTime.of(8, 0), fim = LocalTime.of(12, 0))
        }
    }

    @Test
    fun turma_contida_dentro_da_existente_conflita() {
        given(repository.findAllBySalaIdAndAtivaTrue(salaId))
            .willReturn(listOf(turma(dias = setOf(SEG), inicio = LocalTime.of(8, 0), fim = LocalTime.of(12, 0))))

        assertThrows(ChoqueDeHorarioException::class.java) {
            criarPadrao(dias = setOf(SEG), inicio = LocalTime.of(9, 0), fim = LocalTime.of(10, 0))
        }
    }

    // ---------- choque de horário do professor ----------

    @Test
    fun deve_recusar_professor_escalado_em_outra_turma_no_mesmo_horario() {
        given(repository.findAllBySalaIdAndAtivaTrue(salaId)).willReturn(emptyList())
        given(repository.findAllByProfessorIdAndAtivaTrue(professorId))
            .willReturn(listOf(turma(dias = setOf(SEG), inicio = LocalTime.of(9, 0), fim = LocalTime.of(10, 0))))

        val erro = assertThrows(ChoqueDeHorarioException::class.java) {
            criarPadrao(dias = setOf(SEG), inicio = LocalTime.of(9, 30), fim = LocalTime.of(10, 30))
        }
        assertEquals("Esse professor já está escalado em outra turma nesse dia e horário", erro.message)
    }

    @Test
    fun a_sala_e_validada_antes_do_professor() {
        // Ambos em conflito: a mensagem deve ser a da sala
        given(repository.findAllBySalaIdAndAtivaTrue(salaId)).willReturn(listOf(turma()))
        given(repository.findAllByProfessorIdAndAtivaTrue(professorId)).willReturn(listOf(turma()))

        val erro = assertThrows(ChoqueDeHorarioException::class.java) { criarPadrao() }

        assertEquals("Já existe uma turma nessa sala, nesse dia e horário", erro.message)
    }

    // ---------- atualização ----------

    @Test
    fun atualizar_ignora_a_propria_turma_ao_checar_conflito() {
        val id = UUID.randomUUID()
        val propria = turma(id = id, dias = setOf(SEG), inicio = LocalTime.of(9, 0), fim = LocalTime.of(10, 0))
        given(repository.findById(id)).willReturn(Optional.of(propria))
        given(repository.findAllBySalaIdAndAtivaTrue(salaId)).willReturn(listOf(propria))
        given(repository.findAllByProfessorIdAndAtivaTrue(professorId)).willReturn(listOf(propria))
        given(matriculaRepository.countByTurmaIdAndStatus(any(), any())).willReturn(0)

        val resultado = service.atualizar(
            id, professorId, salaId, setOf(SEG), LocalTime.of(9, 0), LocalTime.of(10, 30), 15
        )

        assertEquals(LocalTime.of(10, 30), resultado.turma.horarioFim)
        assertEquals(15, resultado.turma.capacidadeMaxima)
    }

    @Test
    fun atualizar_ainda_detecta_conflito_com_outra_turma() {
        val id = UUID.randomUUID()
        val propria = turma(id = id)
        val outra = turma(id = UUID.randomUUID(), dias = setOf(SEG), inicio = LocalTime.of(9, 30), fim = LocalTime.of(11, 0))
        given(repository.findById(id)).willReturn(Optional.of(propria))
        given(repository.findAllBySalaIdAndAtivaTrue(salaId)).willReturn(listOf(propria, outra))

        assertThrows(ChoqueDeHorarioException::class.java) {
            service.atualizar(id, professorId, salaId, setOf(SEG), LocalTime.of(9, 0), LocalTime.of(10, 0), 10)
        }
    }

    @Test
    fun atualizar_troca_professor_e_sala_da_turma() {
        val id = UUID.randomUUID()
        val novoProfessor = UUID.randomUUID()
        val novaSala = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(turma(id = id)))
        given(professorRepository.existsById(novoProfessor)).willReturn(true)
        given(salaRepository.existsById(novaSala)).willReturn(true)
        given(professorRepository.findById(novoProfessor))
            .willReturn(Optional.of(ProfessorEntity(id = novoProfessor, nome = "João", telefone = "2", especialidade = null)))
        given(salaRepository.findById(novaSala))
            .willReturn(Optional.of(SalaEntity(id = novaSala, nome = "Sala 2", capacidade = 30)))
        given(repository.findAllBySalaIdAndAtivaTrue(novaSala)).willReturn(emptyList())
        given(repository.findAllByProfessorIdAndAtivaTrue(novoProfessor)).willReturn(emptyList())
        given(matriculaRepository.countByTurmaIdAndStatus(any(), any())).willReturn(0)

        val resultado = service.atualizar(
            id, novoProfessor, novaSala, setOf(TER), LocalTime.of(14, 0), LocalTime.of(15, 0), 12
        )

        assertEquals("João", resultado.professorNome)
        assertEquals("Sala 2", resultado.salaNome)
        verify(repository).save(check {
            assertEquals(novoProfessor, it.professorId)
            assertEquals(novaSala, it.salaId)
            assertEquals(mutableSetOf(TER), it.diasSemana)
        })
    }

    @Test
    fun atualizar_turma_inexistente_falha() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.empty())

        assertThrows(TurmaNaoEncontradaException::class.java) {
            service.atualizar(id, professorId, salaId, setOf(SEG), LocalTime.of(9, 0), LocalTime.of(10, 0), 10)
        }
    }

    // ---------- consultas ----------

    @Test
    fun buscar_por_id_inexistente_falha() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.empty())

        assertThrows(TurmaNaoEncontradaException::class.java) { service.buscarPorId(id) }
    }

    @Test
    fun deve_listar_somente_as_turmas_ativas() {
        given(repository.findAllByAtivaTrue()).willReturn(listOf(turma(), turma()))
        given(matriculaRepository.countByTurmaIdAndStatus(any(), any())).willReturn(0)

        assertEquals(2, service.listarAtivas().size)
    }

    @Test
    fun deve_listar_os_alunos_com_matricula_ativa_na_turma() {
        val turmaId = UUID.randomUUID()
        val alunoId = UUID.randomUUID()
        val matriculaId = UUID.randomUUID()
        given(repository.findById(turmaId)).willReturn(Optional.of(turma(id = turmaId)))
        given(matriculaRepository.findAllByTurmaIdAndStatus(turmaId, StatusMatriculaType.ATIVA)).willReturn(
            listOf(
                MatriculaEntity(
                    id = matriculaId, alunoId = alunoId, turmaId = turmaId,
                    valorMensalidade = BigDecimal("300.00"), diaVencimento = 10,
                    dataInicio = LocalDate.now()
                )
            )
        )
        given(alunoRepository.findById(alunoId)).willReturn(
            Optional.of(
                AlunoEntity(
                    id = alunoId, nome = "Joana", telefone = null,
                    dataNascimento = LocalDate.now().minusYears(20), cpf = null
                )
            )
        )

        val resultado = service.listarAlunosMatriculados(turmaId)

        assertEquals(1, resultado.size)
        assertEquals("Joana", resultado[0].nomeAluno)
        assertEquals(matriculaId, resultado[0].matriculaId)
        assertEquals(StatusMatriculaType.ATIVA, resultado[0].statusMatricula)
    }

    @Test
    fun listar_alunos_de_turma_inexistente_falha() {
        val turmaId = UUID.randomUUID()
        given(repository.findById(turmaId)).willReturn(Optional.empty())

        assertThrows(TurmaNaoEncontradaException::class.java) { service.listarAlunosMatriculados(turmaId) }
        verify(matriculaRepository, never()).findAllByTurmaIdAndStatus(any(), any())
    }

    @Test
    fun listar_alunos_falha_quando_a_matricula_aponta_para_aluno_inexistente() {
        val turmaId = UUID.randomUUID()
        val alunoId = UUID.randomUUID()
        given(repository.findById(turmaId)).willReturn(Optional.of(turma(id = turmaId)))
        given(matriculaRepository.findAllByTurmaIdAndStatus(turmaId, StatusMatriculaType.ATIVA)).willReturn(
            listOf(
                MatriculaEntity(
                    id = UUID.randomUUID(), alunoId = alunoId, turmaId = turmaId,
                    valorMensalidade = BigDecimal("300.00"), diaVencimento = 10, dataInicio = LocalDate.now()
                )
            )
        )
        given(alunoRepository.findById(alunoId)).willReturn(Optional.empty())

        assertThrows(AlunoNaoEncontradoException::class.java) { service.listarAlunosMatriculados(turmaId) }
    }

    // ---------- encerrar e reabrir ----------

    @Test
    fun deve_encerrar_turma_sem_matriculas_ativas() {
        val existente = turma()
        given(repository.findById(existente.id!!)).willReturn(Optional.of(existente))
        given(matriculaRepository.countByTurmaIdAndStatus(existente.id!!, StatusMatriculaType.ATIVA)).willReturn(0)

        val resultado = service.alterarStatus(existente.id!!, ativa = false)

        assertFalse(resultado.turma.ativa)
    }

    /** Encerrar com aluno dentro deixaria o job de mensalidade cobrando por uma turma fora da grade. */
    @Test
    fun nao_deve_encerrar_turma_com_matriculas_ativas() {
        val existente = turma()
        given(repository.findById(existente.id!!)).willReturn(Optional.of(existente))
        given(matriculaRepository.countByTurmaIdAndStatus(existente.id!!, StatusMatriculaType.ATIVA)).willReturn(3)

        assertThrows(TurmaComMatriculasAtivasException::class.java) {
            service.alterarStatus(existente.id!!, ativa = false)
        }
        verify(repository, never()).save(any<TurmaEntity>())
    }

    @Test
    fun deve_reabrir_turma_encerrada() {
        val encerrada = turma().apply { ativa = false }
        given(repository.findById(encerrada.id!!)).willReturn(Optional.of(encerrada))
        given(repository.findAllBySalaIdAndAtivaTrue(salaId)).willReturn(emptyList())
        given(repository.findAllByProfessorIdAndAtivaTrue(professorId)).willReturn(emptyList())

        assertTrue(service.alterarStatus(encerrada.id!!, ativa = true).turma.ativa)
    }

    /** O horário da turma parada pode ter sido tomado enquanto ela estava fora da grade. */
    @Test
    fun nao_deve_reabrir_turma_cujo_horario_foi_ocupado() {
        val encerrada = turma(dias = setOf(SEG), inicio = LocalTime.of(9, 0), fim = LocalTime.of(10, 0))
        encerrada.ativa = false
        val ocupante = turma(dias = setOf(SEG), inicio = LocalTime.of(9, 30), fim = LocalTime.of(10, 30))

        given(repository.findById(encerrada.id!!)).willReturn(Optional.of(encerrada))
        given(repository.findAllBySalaIdAndAtivaTrue(salaId)).willReturn(listOf(ocupante))

        assertThrows(ChoqueDeHorarioException::class.java) {
            service.alterarStatus(encerrada.id!!, ativa = true)
        }
    }

    @Test
    fun alterar_status_para_o_valor_atual_nao_salva() {
        val existente = turma()
        given(repository.findById(existente.id!!)).willReturn(Optional.of(existente))

        assertTrue(service.alterarStatus(existente.id!!, ativa = true).turma.ativa)
        verify(repository, never()).save(any<TurmaEntity>())
    }

    @Test
    fun deve_lancar_excecao_ao_alterar_status_de_turma_inexistente() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.empty())

        assertThrows(TurmaNaoEncontradaException::class.java) { service.alterarStatus(id, ativa = false) }
    }
}
