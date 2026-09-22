package com.crative.studio_api.academico.matricula.service

import com.crative.studio_api.academico.exception.TurmaNaoEncontradaException
import com.crative.studio_api.academico.matricula.entity.MatriculaEntity
import com.crative.studio_api.academico.matricula.exception.AlunoInativoException
import com.crative.studio_api.academico.matricula.exception.AlunoJaMatriculadoNaTurmaException
import com.crative.studio_api.academico.matricula.exception.DadosFinanceirosInvalidosException
import com.crative.studio_api.academico.matricula.exception.MatriculaNaoEncontradaException
import com.crative.studio_api.academico.matricula.exception.ResponsavelObrigatorioException
import com.crative.studio_api.academico.matricula.exception.TransicaoDeStatusInvalidaException
import com.crative.studio_api.academico.matricula.exception.TurmaInativaException
import com.crative.studio_api.academico.matricula.exception.TurmaSemVagaException
import com.crative.studio_api.academico.matricula.repository.MatriculaRepository
import com.crative.studio_api.academico.matricula.types.StatusMatriculaType
import com.crative.studio_api.academico.turma.entity.TurmaEntity
import com.crative.studio_api.academico.turma.repository.TurmaRepository
import com.crative.studio_api.academico.turma.types.DiaSemanaType
import com.crative.studio_api.aluno.entity.AlunoEntity
import com.crative.studio_api.aluno.entity.ResponsavelEntity
import com.crative.studio_api.aluno.exception.AlunoNaoEncontradoException
import com.crative.studio_api.aluno.repository.AlunoRepository
import com.crative.studio_api.aluno.repository.ResponsavelRepository
import com.crative.studio_api.financeiro.service.ContaReceberService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.quality.Strictness
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatriculaServiceTest {

    @Mock lateinit var repository: MatriculaRepository
    @Mock lateinit var turmaRepository: TurmaRepository
    @Mock lateinit var alunoRepository: AlunoRepository
    @Mock lateinit var responsavelRepository: ResponsavelRepository
    @Mock lateinit var contaReceberService: ContaReceberService

    private lateinit var service: MatriculaService

    private val alunoId = UUID.randomUUID()
    private val turmaId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service = MatriculaService(repository, turmaRepository, alunoRepository, responsavelRepository, contaReceberService)

        given(alunoRepository.findById(alunoId)).willReturn(Optional.of(aluno()))
        given(turmaRepository.findById(turmaId)).willReturn(Optional.of(turma()))
        given(repository.findByAlunoIdAndTurmaId(alunoId, turmaId)).willReturn(null)
        given(repository.countByTurmaIdAndStatus(turmaId, StatusMatriculaType.ATIVA)).willReturn(0)
        // save devolve a entidade com id, como o JPA faria
        given(repository.save(any<MatriculaEntity>())).willAnswer { invocacao ->
            val original = invocacao.arguments[0] as MatriculaEntity
            if (original.id != null) original else comId(original)
        }
    }

    private fun comId(m: MatriculaEntity) = MatriculaEntity(
        id = UUID.randomUUID(),
        alunoId = m.alunoId,
        turmaId = m.turmaId,
        status = m.status,
        valorMensalidade = m.valorMensalidade,
        diaVencimento = m.diaVencimento,
        descontoPercentual = m.descontoPercentual,
        dataInicio = m.dataInicio,
        dataFim = m.dataFim
    )

    private fun aluno(
        ativo: Boolean = true,
        dataNascimento: LocalDate = LocalDate.now().minusYears(25)
    ) = AlunoEntity(
        id = alunoId, nome = "Joana", telefone = null,
        dataNascimento = dataNascimento, cpf = "111.111.111-11", ativo = ativo
    )

    private fun turma(ativa: Boolean = true, capacidade: Int = 10) = TurmaEntity(
        id = turmaId, modalidade = "Ballet", professorId = UUID.randomUUID(), salaId = UUID.randomUUID(),
        diasSemana = mutableSetOf(DiaSemanaType.SEG),
        horarioInicio = LocalTime.of(9, 0), horarioFim = LocalTime.of(10, 0),
        capacidadeMaxima = capacidade, ativa = ativa
    )

    private fun matricula(
        id: UUID? = UUID.randomUUID(),
        status: StatusMatriculaType = StatusMatriculaType.ATIVA,
        valor: BigDecimal = BigDecimal("300.00"),
        desconto: BigDecimal = BigDecimal.ZERO,
        dataFim: LocalDate? = null
    ) = MatriculaEntity(
        id = id, alunoId = alunoId, turmaId = turmaId, status = status,
        valorMensalidade = valor, diaVencimento = 10, descontoPercentual = desconto,
        dataInicio = LocalDate.now(), dataFim = dataFim
    )

    private fun criarPadrao(
        valor: BigDecimal = BigDecimal("300.00"),
        diaVencimento: Int = 10,
        desconto: BigDecimal = BigDecimal.ZERO,
        dataInicio: LocalDate = LocalDate.of(2026, 3, 15)
    ) = service.criar(alunoId, turmaId, valor, diaVencimento, desconto, dataInicio)

    // ---------- criação ----------

    @Test
    fun deve_criar_matricula_ativa_com_valor_efetivo_calculado() {
        val resultado = criarPadrao(valor = BigDecimal("300.00"), desconto = BigDecimal("10"))

        assertEquals("Joana", resultado.nomeAluno)
        assertEquals("Ballet", resultado.modalidadeTurma)
        assertEquals(BigDecimal("270.00"), resultado.valorEfetivo)
        assertEquals(StatusMatriculaType.ATIVA, resultado.matricula.status)
    }

    @Test
    fun criar_matricula_gera_a_cobranca_da_competencia_de_inicio() {
        criarPadrao(dataInicio = LocalDate.of(2026, 3, 15))

        verify(contaReceberService).gerarCobrancaMensal(any(), eq(YearMonth.of(2026, 3)))
    }

    @Test
    fun deve_recusar_aluno_inexistente() {
        given(alunoRepository.findById(alunoId)).willReturn(Optional.empty())

        assertThrows(AlunoNaoEncontradoException::class.java) { criarPadrao() }
        verify(repository, never()).save(any<MatriculaEntity>())
    }

    @Test
    fun deve_recusar_aluno_inativo() {
        given(alunoRepository.findById(alunoId)).willReturn(Optional.of(aluno(ativo = false)))

        assertThrows(AlunoInativoException::class.java) { criarPadrao() }
    }

    @Test
    fun deve_recusar_turma_inexistente() {
        given(turmaRepository.findById(turmaId)).willReturn(Optional.empty())

        assertThrows(TurmaNaoEncontradaException::class.java) { criarPadrao() }
    }

    @Test
    fun deve_recusar_turma_inativa() {
        given(turmaRepository.findById(turmaId)).willReturn(Optional.of(turma(ativa = false)))

        assertThrows(TurmaInativaException::class.java) { criarPadrao() }
    }

    @Test
    fun deve_recusar_turma_sem_vaga() {
        given(turmaRepository.findById(turmaId)).willReturn(Optional.of(turma(capacidade = 5)))
        given(repository.countByTurmaIdAndStatus(turmaId, StatusMatriculaType.ATIVA)).willReturn(5)

        assertThrows(TurmaSemVagaException::class.java) { criarPadrao() }
        verify(contaReceberService, never()).gerarCobrancaMensal(any(), any())
    }

    @Test
    fun deve_aceitar_a_matricula_que_ocupa_a_ultima_vaga() {
        given(turmaRepository.findById(turmaId)).willReturn(Optional.of(turma(capacidade = 5)))
        given(repository.countByTurmaIdAndStatus(turmaId, StatusMatriculaType.ATIVA)).willReturn(4)

        assertEquals(StatusMatriculaType.ATIVA, criarPadrao().matricula.status)
    }

    // ---------- responsável de menor de idade ----------

    @Test
    fun deve_recusar_menor_de_idade_sem_responsavel() {
        given(alunoRepository.findById(alunoId))
            .willReturn(Optional.of(aluno(dataNascimento = LocalDate.now().minusYears(10))))
        given(responsavelRepository.findAllByAlunoId(alunoId)).willReturn(emptyList())

        assertThrows(ResponsavelObrigatorioException::class.java) { criarPadrao() }
    }

    @Test
    fun deve_aceitar_menor_de_idade_com_responsavel() {
        given(alunoRepository.findById(alunoId))
            .willReturn(Optional.of(aluno(dataNascimento = LocalDate.now().minusYears(10))))
        given(responsavelRepository.findAllByAlunoId(alunoId)).willReturn(
            listOf(ResponsavelEntity(alunoId = alunoId, nome = "Maria", telefone = "1", parentesco = "Mãe"))
        )

        assertEquals(StatusMatriculaType.ATIVA, criarPadrao().matricula.status)
    }

    @Test
    fun maior_de_idade_nao_precisa_de_responsavel() {
        criarPadrao()

        verify(responsavelRepository, never()).findAllByAlunoId(any())
    }

    // ---------- dados financeiros ----------

    @Test
    fun deve_recusar_mensalidade_zero() {
        assertThrows(DadosFinanceirosInvalidosException::class.java) {
            criarPadrao(valor = BigDecimal.ZERO)
        }
        // a validação financeira roda antes de qualquer consulta
        verify(alunoRepository, never()).findById(any())
    }

    @Test
    fun deve_recusar_mensalidade_negativa() {
        assertThrows(DadosFinanceirosInvalidosException::class.java) {
            criarPadrao(valor = BigDecimal("-1"))
        }
    }

    @Test
    fun deve_recusar_dia_de_vencimento_fora_de_um_a_vinte_e_oito() {
        assertThrows(DadosFinanceirosInvalidosException::class.java) { criarPadrao(diaVencimento = 0) }
        assertThrows(DadosFinanceirosInvalidosException::class.java) { criarPadrao(diaVencimento = 29) }
    }

    @Test
    fun deve_aceitar_os_extremos_do_dia_de_vencimento() {
        assertEquals(1, criarPadrao(diaVencimento = 1).matricula.diaVencimento)
        assertEquals(28, criarPadrao(diaVencimento = 28).matricula.diaVencimento)
    }

    @Test
    fun deve_recusar_desconto_negativo_ou_acima_de_cem() {
        assertThrows(DadosFinanceirosInvalidosException::class.java) {
            criarPadrao(desconto = BigDecimal("-0.01"))
        }
        assertThrows(DadosFinanceirosInvalidosException::class.java) {
            criarPadrao(desconto = BigDecimal("100.01"))
        }
    }

    @Test
    fun deve_aceitar_desconto_de_cem_por_cento() {
        assertEquals(BigDecimal("0.00"), criarPadrao(desconto = BigDecimal("100")).valorEfetivo)
    }

    // ---------- duplicidade e rematrícula ----------

    @Test
    fun deve_recusar_aluno_com_matricula_ativa_na_mesma_turma() {
        given(repository.findByAlunoIdAndTurmaId(alunoId, turmaId))
            .willReturn(matricula(status = StatusMatriculaType.ATIVA))

        assertThrows(AlunoJaMatriculadoNaTurmaException::class.java) { criarPadrao() }
    }

    @Test
    fun deve_recusar_aluno_com_matricula_trancada_na_mesma_turma() {
        given(repository.findByAlunoIdAndTurmaId(alunoId, turmaId))
            .willReturn(matricula(status = StatusMatriculaType.TRANCADA))

        assertThrows(AlunoJaMatriculadoNaTurmaException::class.java) { criarPadrao() }
    }

    @Test
    fun rematricula_reaproveita_a_linha_cancelada_com_os_novos_valores() {
        val cancelada = matricula(
            status = StatusMatriculaType.CANCELADA,
            valor = BigDecimal("300.00"),
            dataFim = LocalDate.of(2026, 1, 31)
        )
        given(repository.findByAlunoIdAndTurmaId(alunoId, turmaId)).willReturn(cancelada)

        val resultado = criarPadrao(
            valor = BigDecimal("400.00"),
            desconto = BigDecimal("25"),
            dataInicio = LocalDate.of(2026, 5, 1)
        )

        assertEquals(cancelada.id, resultado.matricula.id)
        assertEquals(StatusMatriculaType.ATIVA, resultado.matricula.status)
        assertEquals(BigDecimal("400.00"), resultado.matricula.valorMensalidade)
        assertEquals(BigDecimal("300.00"), resultado.valorEfetivo)
        assertEquals(LocalDate.of(2026, 5, 1), resultado.matricula.dataInicio)
        assertNull(resultado.matricula.dataFim)
        verify(contaReceberService).gerarCobrancaMensal(any(), eq(YearMonth.of(2026, 5)))
    }

    @Test
    fun rematricula_ainda_respeita_a_capacidade_da_turma() {
        given(repository.findByAlunoIdAndTurmaId(alunoId, turmaId))
            .willReturn(matricula(status = StatusMatriculaType.CANCELADA))
        given(turmaRepository.findById(turmaId)).willReturn(Optional.of(turma(capacidade = 3)))
        given(repository.countByTurmaIdAndStatus(turmaId, StatusMatriculaType.ATIVA)).willReturn(3)

        assertThrows(TurmaSemVagaException::class.java) { criarPadrao() }
    }

    // ---------- consultas ----------

    @Test
    fun buscar_por_id_inexistente_falha() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.empty())

        assertThrows(MatriculaNaoEncontradaException::class.java) { service.buscarPorId(id) }
    }

    @Test
    fun deve_listar_matriculas_do_aluno() {
        given(repository.findAllByAlunoId(alunoId)).willReturn(listOf(matricula(), matricula()))

        assertEquals(2, service.listarPorAluno(alunoId).size)
    }

    @Test
    fun listar_por_aluno_inexistente_falha() {
        given(alunoRepository.findById(alunoId)).willReturn(Optional.empty())

        assertThrows(AlunoNaoEncontradoException::class.java) { service.listarPorAluno(alunoId) }
        verify(repository, never()).findAllByAlunoId(any())
    }

    @Test
    fun deve_listar_matriculas_da_turma() {
        given(repository.findAllByTurmaId(turmaId)).willReturn(listOf(matricula()))

        assertEquals(1, service.listarPorTurma(turmaId).size)
    }

    @Test
    fun listar_por_turma_inexistente_falha() {
        given(turmaRepository.findById(turmaId)).willReturn(Optional.empty())

        assertThrows(TurmaNaoEncontradaException::class.java) { service.listarPorTurma(turmaId) }
    }

    @Test
    fun deve_listar_matriculas_por_status() {
        given(repository.findAllByStatus(StatusMatriculaType.TRANCADA))
            .willReturn(listOf(matricula(status = StatusMatriculaType.TRANCADA)))

        val resultado = service.listarPorStatus(StatusMatriculaType.TRANCADA)

        assertEquals(StatusMatriculaType.TRANCADA, resultado.single().matricula.status)
    }

    // ---------- transições de status ----------

    @Test
    fun deve_trancar_matricula_ativa_sem_gravar_data_fim() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(matricula(id = id, status = StatusMatriculaType.ATIVA)))

        val resultado = service.alterarStatus(id, StatusMatriculaType.TRANCADA)

        assertEquals(StatusMatriculaType.TRANCADA, resultado.matricula.status)
        assertNull(resultado.matricula.dataFim)
    }

    @Test
    fun deve_cancelar_gravando_a_data_fim() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(matricula(id = id, status = StatusMatriculaType.ATIVA)))

        val resultado = service.alterarStatus(id, StatusMatriculaType.CANCELADA)

        assertEquals(StatusMatriculaType.CANCELADA, resultado.matricula.status)
        assertEquals(LocalDate.now(), resultado.matricula.dataFim)
    }

    @Test
    fun deve_reativar_matricula_trancada_limpando_a_data_fim() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(
            Optional.of(matricula(id = id, status = StatusMatriculaType.TRANCADA, dataFim = LocalDate.now()))
        )

        val resultado = service.alterarStatus(id, StatusMatriculaType.ATIVA)

        assertEquals(StatusMatriculaType.ATIVA, resultado.matricula.status)
        assertNull(resultado.matricula.dataFim)
    }

    @Test
    fun reativar_exige_turma_ativa() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(matricula(id = id, status = StatusMatriculaType.TRANCADA)))
        given(turmaRepository.findById(turmaId)).willReturn(Optional.of(turma(ativa = false)))

        assertThrows(TurmaInativaException::class.java) { service.alterarStatus(id, StatusMatriculaType.ATIVA) }
    }

    @Test
    fun reativar_exige_vaga_livre() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(matricula(id = id, status = StatusMatriculaType.TRANCADA)))
        given(turmaRepository.findById(turmaId)).willReturn(Optional.of(turma(capacidade = 2)))
        given(repository.countByTurmaIdAndStatus(turmaId, StatusMatriculaType.ATIVA)).willReturn(2)

        assertThrows(TurmaSemVagaException::class.java) { service.alterarStatus(id, StatusMatriculaType.ATIVA) }
    }

    @Test
    fun trancar_nao_checa_vaga_nem_turma_ativa() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(matricula(id = id, status = StatusMatriculaType.ATIVA)))
        given(turmaRepository.findById(turmaId)).willReturn(Optional.of(turma(ativa = false, capacidade = 1)))
        given(repository.countByTurmaIdAndStatus(turmaId, StatusMatriculaType.ATIVA)).willReturn(99)

        assertEquals(
            StatusMatriculaType.TRANCADA,
            service.alterarStatus(id, StatusMatriculaType.TRANCADA).matricula.status
        )
    }

    @Test
    fun deve_recusar_transicao_para_o_mesmo_status() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(matricula(id = id, status = StatusMatriculaType.ATIVA)))

        val erro = assertThrows(TransicaoDeStatusInvalidaException::class.java) {
            service.alterarStatus(id, StatusMatriculaType.ATIVA)
        }
        assertEquals("A matrícula já está com status ATIVA", erro.message)
    }

    @Test
    fun cancelada_e_terminal() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(matricula(id = id, status = StatusMatriculaType.CANCELADA)))

        assertThrows(TransicaoDeStatusInvalidaException::class.java) {
            service.alterarStatus(id, StatusMatriculaType.ATIVA)
        }
        assertThrows(TransicaoDeStatusInvalidaException::class.java) {
            service.alterarStatus(id, StatusMatriculaType.TRANCADA)
        }
        verify(repository, never()).save(any<MatriculaEntity>())
    }

    @Test
    fun alterar_status_de_matricula_inexistente_falha() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.empty())

        assertThrows(MatriculaNaoEncontradaException::class.java) {
            service.alterarStatus(id, StatusMatriculaType.CANCELADA)
        }
    }

    @Test
    fun alterar_status_nao_gera_cobranca() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(matricula(id = id, status = StatusMatriculaType.ATIVA)))

        service.alterarStatus(id, StatusMatriculaType.TRANCADA)

        verify(contaReceberService, never()).gerarCobrancaMensal(any(), any())
    }

    @Test
    fun a_cobranca_recebe_a_matricula_ja_persistida_com_id() {
        criarPadrao()

        verify(contaReceberService).gerarCobrancaMensal(check { assertNotNull(it.id) }, any())
    }

    // ---------- listagem com filtros combináveis ----------

    /** O bug que a mudança fecha: antes o status era descartado quando vinha junto de turmaId. */
    @Test
    fun deve_combinar_turma_e_status_em_vez_de_ignorar_o_status() {
        val ativa = matricula(status = StatusMatriculaType.ATIVA)
        val cancelada = matricula(status = StatusMatriculaType.CANCELADA)
        given(repository.findAllByTurmaId(turmaId)).willReturn(listOf(ativa, cancelada))

        val resultado = service.listar(alunoId = null, turmaId = turmaId, status = StatusMatriculaType.ATIVA)

        assertEquals(1, resultado.size)
        assertEquals(StatusMatriculaType.ATIVA, resultado.single().matricula.status)
    }

    @Test
    fun deve_combinar_aluno_e_status() {
        val ativa = matricula(status = StatusMatriculaType.ATIVA)
        val trancada = matricula(status = StatusMatriculaType.TRANCADA)
        given(repository.findAllByAlunoId(alunoId)).willReturn(listOf(ativa, trancada))

        val resultado = service.listar(alunoId, turmaId = null, status = StatusMatriculaType.TRANCADA)

        assertEquals(StatusMatriculaType.TRANCADA, resultado.single().matricula.status)
    }

    /** Sem status, a ficha do aluno traz tudo — inclusive canceladas. */
    @Test
    fun deve_trazer_todos_os_status_ao_filtrar_so_por_aluno() {
        given(repository.findAllByAlunoId(alunoId))
            .willReturn(listOf(matricula(status = StatusMatriculaType.ATIVA), matricula(status = StatusMatriculaType.CANCELADA)))

        assertEquals(2, service.listar(alunoId, turmaId = null, status = null).size)
    }

    @Test
    fun sem_nenhum_filtro_devolve_apenas_as_ativas() {
        given(repository.findAllByStatus(StatusMatriculaType.ATIVA)).willReturn(listOf(matricula()))

        val resultado = service.listar(alunoId = null, turmaId = null, status = null)

        assertEquals(1, resultado.size)
        verify(repository).findAllByStatus(StatusMatriculaType.ATIVA)
    }

    /** Id inexistente é 404, e não lista vazia — que a tela leria como "não tem matrícula". */
    @Test
    fun deve_lancar_404_ao_filtrar_por_aluno_inexistente() {
        val inexistente = UUID.randomUUID()
        given(alunoRepository.findById(inexistente)).willReturn(Optional.empty())

        assertThrows(AlunoNaoEncontradoException::class.java) {
            service.listar(inexistente, turmaId = null, status = null)
        }
    }

    @Test
    fun deve_lancar_404_ao_filtrar_por_turma_inexistente() {
        val inexistente = UUID.randomUUID()
        given(turmaRepository.findById(inexistente)).willReturn(Optional.empty())

        assertThrows(TurmaNaoEncontradaException::class.java) {
            service.listar(alunoId = null, turmaId = inexistente, status = null)
        }
    }
}
