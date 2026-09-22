package com.crative.studio_api.financeiro.service

import com.crative.studio_api.academico.matricula.entity.MatriculaEntity
import com.crative.studio_api.academico.matricula.exception.MatriculaNaoEncontradaException
import com.crative.studio_api.academico.matricula.repository.MatriculaRepository
import com.crative.studio_api.aluno.entity.AlunoEntity
import com.crative.studio_api.aluno.exception.AlunoNaoEncontradoException
import com.crative.studio_api.aluno.repository.AlunoRepository
import com.crative.studio_api.financeiro.entity.ContaReceberEntity
import com.crative.studio_api.financeiro.exception.ContaReceberNaoEncontradaException
import com.crative.studio_api.financeiro.repository.ContaReceberRepository
import com.crative.studio_api.financeiro.types.StatusContaType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.quality.Strictness
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContaReceberServiceTest {

    @Mock lateinit var repository: ContaReceberRepository
    @Mock lateinit var matriculaRepository: MatriculaRepository
    @Mock lateinit var alunoRepository: AlunoRepository

    private lateinit var service: ContaReceberService

    private val matriculaId = UUID.randomUUID()
    private val alunoId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service = ContaReceberService(repository, matriculaRepository, alunoRepository)
        given(repository.save(any<ContaReceberEntity>())).willAnswer { it.arguments[0] }
    }

    private fun matricula(
        valor: BigDecimal = BigDecimal("300.00"),
        desconto: BigDecimal = BigDecimal.ZERO,
        diaVencimento: Int = 10
    ) = MatriculaEntity(
        id = matriculaId, alunoId = alunoId, turmaId = UUID.randomUUID(),
        valorMensalidade = valor, diaVencimento = diaVencimento,
        descontoPercentual = desconto, dataInicio = LocalDate.now()
    )

    private fun conta(
        id: UUID? = UUID.randomUUID(),
        referencia: String = "2026-04",
        status: StatusContaType = StatusContaType.PENDENTE,
        vencimento: LocalDate = LocalDate.now().plusDays(5),
        valor: BigDecimal = BigDecimal("300.00")
    ) = ContaReceberEntity(
        id = id, matriculaId = matriculaId, referencia = referencia,
        valor = valor, vencimento = vencimento, status = status
    )

    // ---------- geração de cobrança ----------

    @Test
    fun deve_gerar_cobranca_com_vencimento_no_dia_da_matricula() {
        given(repository.existsByMatriculaIdAndReferencia(matriculaId, "2026-04")).willReturn(false)

        val resultado = service.gerarCobrancaMensal(matricula(diaVencimento = 10), YearMonth.of(2026, 4))

        assertEquals("2026-04", resultado.referencia)
        assertEquals(LocalDate.of(2026, 4, 10), resultado.vencimento)
        assertEquals(matriculaId, resultado.matriculaId)
    }

    @Test
    fun a_cobranca_copia_o_valor_efetivo_e_nao_a_mensalidade_cheia() {
        given(repository.existsByMatriculaIdAndReferencia(any(), any())).willReturn(false)

        val resultado = service.gerarCobrancaMensal(
            matricula(valor = BigDecimal("400.00"), desconto = BigDecimal("25")),
            YearMonth.of(2026, 4)
        )

        assertEquals(BigDecimal("300.00"), resultado.valor)
    }

    @Test
    fun cobranca_de_competencia_futura_nasce_pendente() {
        given(repository.existsByMatriculaIdAndReferencia(any(), any())).willReturn(false)
        val futuro = YearMonth.from(LocalDate.now().plusMonths(2))

        val resultado = service.gerarCobrancaMensal(matricula(), futuro)

        assertEquals(StatusContaType.PENDENTE, resultado.status)
    }

    @Test
    fun cobranca_retroativa_nasce_atrasada() {
        given(repository.existsByMatriculaIdAndReferencia(any(), any())).willReturn(false)
        val passado = YearMonth.from(LocalDate.now().minusMonths(2))

        val resultado = service.gerarCobrancaMensal(matricula(), passado)

        assertEquals(StatusContaType.ATRASADO, resultado.status)
        assertTrue(resultado.vencimento.isBefore(LocalDate.now()))
    }

    @Test
    fun gerar_duas_vezes_a_mesma_competencia_devolve_a_conta_existente_sem_inserir() {
        val existente = conta(referencia = "2026-04")
        given(repository.existsByMatriculaIdAndReferencia(matriculaId, "2026-04")).willReturn(true)
        given(repository.findAllByMatriculaId(matriculaId)).willReturn(listOf(existente))

        val resultado = service.gerarCobrancaMensal(matricula(), YearMonth.of(2026, 4))

        assertEquals(existente.id, resultado.id)
        verify(repository, never()).save(any<ContaReceberEntity>())
    }

    @Test
    fun a_idempotencia_escolhe_a_conta_da_competencia_certa() {
        given(repository.existsByMatriculaIdAndReferencia(matriculaId, "2026-05")).willReturn(true)
        given(repository.findAllByMatriculaId(matriculaId)).willReturn(
            listOf(conta(referencia = "2026-03"), conta(referencia = "2026-04"), conta(referencia = "2026-05"))
        )

        val resultado = service.gerarCobrancaMensal(matricula(), YearMonth.of(2026, 5))

        assertEquals("2026-05", resultado.referencia)
    }

    @Test
    fun a_referencia_e_formatada_com_mes_de_dois_digitos() {
        given(repository.existsByMatriculaIdAndReferencia(any(), any())).willReturn(false)

        assertEquals("2026-01", service.gerarCobrancaMensal(matricula(), YearMonth.of(2026, 1)).referencia)
    }

    @Test
    fun deve_falhar_ao_gerar_cobranca_de_matricula_sem_id() {
        val semId = MatriculaEntity(
            id = null, alunoId = alunoId, turmaId = UUID.randomUUID(),
            valorMensalidade = BigDecimal("300.00"), diaVencimento = 10, dataInicio = LocalDate.now()
        )

        assertThrows(IllegalArgumentException::class.java) {
            service.gerarCobrancaMensal(semId, YearMonth.of(2026, 4))
        }
    }

    // ---------- detalhamento ----------

    @Test
    fun deve_detalhar_a_conta_com_o_aluno_da_matricula() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(conta(id = id)))
        given(matriculaRepository.findById(matriculaId)).willReturn(Optional.of(matricula()))
        given(alunoRepository.findById(alunoId)).willReturn(Optional.of(alunoEntity()))

        val resultado = service.buscarPorId(id)

        assertEquals("Joana", resultado.nomeAluno)
        assertEquals(alunoId, resultado.alunoId)
    }

    @Test
    fun conta_pendente_nao_tem_dias_em_atraso() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(conta(id = id, status = StatusContaType.PENDENTE)))
        given(matriculaRepository.findById(matriculaId)).willReturn(Optional.of(matricula()))
        given(alunoRepository.findById(alunoId)).willReturn(Optional.of(alunoEntity()))

        assertNull(service.buscarPorId(id).diasEmAtraso)
    }

    @Test
    fun conta_paga_nao_tem_dias_em_atraso_mesmo_vencida() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(
            Optional.of(conta(id = id, status = StatusContaType.PAGO, vencimento = LocalDate.now().minusDays(30)))
        )
        given(matriculaRepository.findById(matriculaId)).willReturn(Optional.of(matricula()))
        given(alunoRepository.findById(alunoId)).willReturn(Optional.of(alunoEntity()))

        assertNull(service.buscarPorId(id).diasEmAtraso)
    }

    @Test
    fun conta_atrasada_informa_os_dias_de_atraso() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(
            Optional.of(conta(id = id, status = StatusContaType.ATRASADO, vencimento = LocalDate.now().minusDays(7)))
        )
        given(matriculaRepository.findById(matriculaId)).willReturn(Optional.of(matricula()))
        given(alunoRepository.findById(alunoId)).willReturn(Optional.of(alunoEntity()))

        assertEquals(7, service.buscarPorId(id).diasEmAtraso)
    }

    @Test
    fun buscar_conta_inexistente_falha() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.empty())

        assertThrows(ContaReceberNaoEncontradaException::class.java) { service.buscarPorId(id) }
    }

    @Test
    fun detalhar_falha_quando_a_matricula_vinculada_nao_existe() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(conta(id = id)))
        given(matriculaRepository.findById(matriculaId)).willReturn(Optional.empty())

        assertThrows(MatriculaNaoEncontradaException::class.java) { service.buscarPorId(id) }
    }

    @Test
    fun detalhar_falha_quando_o_aluno_vinculado_nao_existe() {
        val id = UUID.randomUUID()
        given(repository.findById(id)).willReturn(Optional.of(conta(id = id)))
        given(matriculaRepository.findById(matriculaId)).willReturn(Optional.of(matricula()))
        given(alunoRepository.findById(alunoId)).willReturn(Optional.empty())

        assertThrows(AlunoNaoEncontradoException::class.java) { service.buscarPorId(id) }
    }

    // ---------- listagem ----------

    @Test
    fun listar_combina_matricula_e_status_quando_os_dois_vem() {
        given(repository.findAllByMatriculaIdAndStatus(matriculaId, StatusContaType.PENDENTE))
            .willReturn(listOf(conta()))
        stubDetalhamento()

        assertEquals(1, service.listar(StatusContaType.PENDENTE, matriculaId).size)
        verify(repository, never()).findAllByMatriculaId(any())
        verify(repository, never()).findAllByStatus(any())
        verify(repository, never()).findAll()
    }

    @Test
    fun listar_so_por_matricula() {
        given(repository.findAllByMatriculaId(matriculaId)).willReturn(listOf(conta(), conta()))
        stubDetalhamento()

        assertEquals(2, service.listar(null, matriculaId).size)
    }

    @Test
    fun listar_so_por_status() {
        given(repository.findAllByStatus(StatusContaType.ATRASADO))
            .willReturn(listOf(conta(status = StatusContaType.ATRASADO)))
        stubDetalhamento()

        assertEquals(1, service.listar(StatusContaType.ATRASADO, null).size)
    }

    @Test
    fun listar_sem_filtro_devolve_todas() {
        given(repository.findAll()).willReturn(listOf(conta(), conta(), conta()))
        stubDetalhamento()

        assertEquals(3, service.listar(null, null).size)
    }

    // ---------- baixa e job de atraso ----------

    @Test
    fun marcar_como_pago_muda_o_status() {
        val alvo = conta(status = StatusContaType.ATRASADO)

        assertEquals(StatusContaType.PAGO, service.marcarComoPago(alvo).status)
    }

    @Test
    fun marcar_contas_atrasadas_atualiza_as_pendentes_vencidas() {
        val vencidas = listOf(
            conta(status = StatusContaType.PENDENTE, vencimento = LocalDate.now().minusDays(1)),
            conta(status = StatusContaType.PENDENTE, vencimento = LocalDate.now().minusDays(40))
        )
        given(repository.findAllByStatusAndVencimentoBefore(StatusContaType.PENDENTE, LocalDate.now()))
            .willReturn(vencidas)
        given(repository.saveAll(any<List<ContaReceberEntity>>())).willAnswer { it.arguments[0] }

        val resultado = service.marcarContasAtrasadas()

        assertEquals(2, resultado.size)
        assertTrue(resultado.all { it.status == StatusContaType.ATRASADO })
    }

    @Test
    fun marcar_contas_atrasadas_sem_vencidas_nao_muda_nada() {
        given(repository.findAllByStatusAndVencimentoBefore(any(), any())).willReturn(emptyList())
        given(repository.saveAll(any<List<ContaReceberEntity>>())).willAnswer { it.arguments[0] }

        assertTrue(service.marcarContasAtrasadas().isEmpty())
    }

    private fun alunoEntity() = AlunoEntity(
        id = alunoId, nome = "Joana", telefone = null,
        dataNascimento = LocalDate.now().minusYears(20), cpf = null
    )

    private fun stubDetalhamento() {
        given(matriculaRepository.findById(matriculaId)).willReturn(Optional.of(matricula()))
        given(alunoRepository.findById(alunoId)).willReturn(Optional.of(alunoEntity()))
    }
}
