package com.crative.studio_api.aluno.service

import com.crative.studio_api.aluno.entity.AlunoEntity
import com.crative.studio_api.aluno.entity.ResponsavelEntity
import com.crative.studio_api.aluno.exception.AlunoNaoEncontradoException
import com.crative.studio_api.aluno.exception.ResponsavelNaoEncontradoException
import com.crative.studio_api.aluno.exception.UltimoResponsavelDeMenorException
import com.crative.studio_api.academico.matricula.entity.MatriculaEntity
import com.crative.studio_api.academico.matricula.types.StatusMatriculaType
import com.crative.studio_api.academico.matricula.repository.MatriculaRepository
import com.crative.studio_api.aluno.repository.AlunoRepository
import com.crative.studio_api.aluno.repository.ResponsavelRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ResponsavelServiceTest {

    @Mock
    lateinit var responsavelRepository: ResponsavelRepository

    @Mock
    lateinit var alunoRepository: AlunoRepository

    @Mock
    lateinit var matriculaRepository: MatriculaRepository

    private lateinit var service: ResponsavelService

    @BeforeEach
    fun setUp() {
        service = ResponsavelService(responsavelRepository, alunoRepository, matriculaRepository)
    }

    private val alunoId = UUID.randomUUID()

    private fun aluno() = AlunoEntity(
        id = alunoId,
        nome = "Joana",
        telefone = null,
        dataNascimento = LocalDate.now().minusYears(10),
        cpf = "111.111.111-11"
    )

    @Test
    fun deve_cadastrar_responsavel_vinculado_ao_aluno() {
        given(alunoRepository.findById(alunoId)).willReturn(Optional.of(aluno()))
        given(responsavelRepository.save(any<ResponsavelEntity>())).willAnswer { it.arguments[0] }

        val resultado = service.cadastrar(alunoId, "Maria", "11977776666", "222.222.222-22", "Mãe")

        assertEquals("Maria", resultado.nome)
        verify(responsavelRepository).save(check {
            assertEquals(alunoId, it.alunoId)
            assertEquals("Mãe", it.parentesco)
        })
    }

    @Test
    fun deve_aceitar_responsavel_sem_cpf() {
        given(alunoRepository.findById(alunoId)).willReturn(Optional.of(aluno()))
        given(responsavelRepository.save(any<ResponsavelEntity>())).willAnswer { it.arguments[0] }

        assertNull(service.cadastrar(alunoId, "Maria", "11977776666", null, "Mãe").cpf)
    }

    @Test
    fun deve_recusar_cadastro_para_aluno_inexistente() {
        given(alunoRepository.findById(alunoId)).willReturn(Optional.empty())

        assertThrows(AlunoNaoEncontradoException::class.java) {
            service.cadastrar(alunoId, "Maria", "11977776666", null, "Mãe")
        }
        verify(responsavelRepository, never()).save(any<ResponsavelEntity>())
    }

    @Test
    fun deve_listar_responsaveis_do_aluno() {
        given(alunoRepository.findById(alunoId)).willReturn(Optional.of(aluno()))
        given(responsavelRepository.findAllByAlunoId(alunoId)).willReturn(
            listOf(
                ResponsavelEntity(alunoId = alunoId, nome = "Maria", telefone = "1", parentesco = "Mãe"),
                ResponsavelEntity(alunoId = alunoId, nome = "José", telefone = "2", parentesco = "Pai")
            )
        )

        assertEquals(listOf("Maria", "José"), service.listarPorAluno(alunoId).map { it.nome })
    }

    @Test
    fun deve_recusar_listagem_para_aluno_inexistente() {
        given(alunoRepository.findById(alunoId)).willReturn(Optional.empty())

        assertThrows(AlunoNaoEncontradoException::class.java) { service.listarPorAluno(alunoId) }
        verify(responsavelRepository, never()).findAllByAlunoId(any())
    }

    @Test
    fun deve_atualizar_os_dados_do_responsavel() {
        val id = UUID.randomUUID()
        val existente = ResponsavelEntity(
            id = id, alunoId = alunoId, nome = "Maria", telefone = "1", cpf = null, parentesco = "Mãe"
        )
        given(responsavelRepository.findById(id)).willReturn(Optional.of(existente))
        given(responsavelRepository.save(any<ResponsavelEntity>())).willAnswer { it.arguments[0] }

        val resultado = service.atualizar(id, "Maria Silva", "11911112222", "333.333.333-33", "Avó")

        assertEquals("Maria Silva", resultado.nome)
        assertEquals("11911112222", resultado.telefone)
        assertEquals("333.333.333-33", resultado.cpf)
        assertEquals("Avó", resultado.parentesco)
        // o vínculo com o aluno não muda numa atualização
        assertEquals(alunoId, resultado.alunoId)
    }

    @Test
    fun deve_lancar_excecao_ao_atualizar_responsavel_inexistente() {
        val id = UUID.randomUUID()
        given(responsavelRepository.findById(id)).willReturn(Optional.empty())

        assertThrows(ResponsavelNaoEncontradoException::class.java) {
            service.atualizar(id, "Maria", "1", null, "Mãe")
        }
    }

    // --- remoção ---

    private val responsavelId = UUID.randomUUID()

    private fun responsavel() = ResponsavelEntity(
        id = responsavelId, alunoId = alunoId, nome = "Maria",
        telefone = "11999999999", cpf = null, parentesco = "Mãe"
    )

    private fun alunoComIdade(anos: Long) = AlunoEntity(
        id = alunoId, nome = "Joana", telefone = null,
        dataNascimento = LocalDate.now().minusYears(anos), cpf = "111.111.111-11"
    )

    private fun matriculaAtiva() = MatriculaEntity(
        id = UUID.randomUUID(), alunoId = alunoId, turmaId = UUID.randomUUID(),
        status = StatusMatriculaType.ATIVA, valorMensalidade = BigDecimal("300.00"),
        diaVencimento = 10, dataInicio = LocalDate.now()
    )

    @Test
    fun deve_remover_responsavel_quando_ha_outro_cadastrado() {
        given(responsavelRepository.findById(responsavelId)).willReturn(Optional.of(responsavel()))
        given(responsavelRepository.countByAlunoId(alunoId)).willReturn(2)

        service.remover(responsavelId)

        verify(responsavelRepository).delete(any<ResponsavelEntity>())
    }

    /**
     * A trava que fecha o buraco: a matrícula só exige responsável no momento da criação, então sem
     * isto dava para esvaziar a lista depois e deixar o menor matriculado sem ninguém por ele.
     */
    @Test
    fun nao_deve_remover_ultimo_responsavel_de_menor_com_matricula_ativa() {
        given(responsavelRepository.findById(responsavelId)).willReturn(Optional.of(responsavel()))
        given(responsavelRepository.countByAlunoId(alunoId)).willReturn(1)
        given(alunoRepository.findById(alunoId)).willReturn(Optional.of(alunoComIdade(10)))
        given(matriculaRepository.findAllByAlunoIdAndStatus(alunoId, StatusMatriculaType.ATIVA))
            .willReturn(listOf(matriculaAtiva()))

        assertThrows(UltimoResponsavelDeMenorException::class.java) { service.remover(responsavelId) }
        verify(responsavelRepository, never()).delete(any<ResponsavelEntity>())
    }

    /** Sem matrícula ativa não há o que proteger — a ficha do menor pode ficar sem responsável. */
    @Test
    fun deve_remover_ultimo_responsavel_de_menor_sem_matricula_ativa() {
        given(responsavelRepository.findById(responsavelId)).willReturn(Optional.of(responsavel()))
        given(responsavelRepository.countByAlunoId(alunoId)).willReturn(1)
        given(alunoRepository.findById(alunoId)).willReturn(Optional.of(alunoComIdade(10)))
        given(matriculaRepository.findAllByAlunoIdAndStatus(alunoId, StatusMatriculaType.ATIVA))
            .willReturn(emptyList())

        service.remover(responsavelId)

        verify(responsavelRepository).delete(any<ResponsavelEntity>())
    }

    /** Maior de idade responde por si: a regra de responsável obrigatório nunca o alcançou. */
    @Test
    fun deve_remover_ultimo_responsavel_de_aluno_maior_de_idade() {
        given(responsavelRepository.findById(responsavelId)).willReturn(Optional.of(responsavel()))
        given(responsavelRepository.countByAlunoId(alunoId)).willReturn(1)
        given(alunoRepository.findById(alunoId)).willReturn(Optional.of(alunoComIdade(25)))

        service.remover(responsavelId)

        verify(responsavelRepository).delete(any<ResponsavelEntity>())
    }

    @Test
    fun deve_lancar_excecao_ao_remover_responsavel_inexistente() {
        given(responsavelRepository.findById(responsavelId)).willReturn(Optional.empty())

        assertThrows(ResponsavelNaoEncontradoException::class.java) { service.remover(responsavelId) }
    }
}
