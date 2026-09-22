package com.crative.studio_api.usuario.service

import com.crative.studio_api.professor.entity.ProfessorEntity
import com.crative.studio_api.professor.repository.ProfessorRepository
import com.crative.studio_api.professor.service.ProfessorService
import com.crative.studio_api.usuario.dto.request.CompletarCadastroRequest
import com.crative.studio_api.usuario.entity.ConviteUsuarioEntity
import com.crative.studio_api.usuario.entity.RoleType
import com.crative.studio_api.usuario.entity.StatusConvite
import com.crative.studio_api.usuario.entity.UsuarioEntity
import com.crative.studio_api.usuario.exception.ConviteInvalidoException
import com.crative.studio_api.usuario.exception.ConviteNaoEncontradoException
import com.crative.studio_api.usuario.exception.EmailJaExisteNaBaseException
import com.crative.studio_api.usuario.exception.ProfessorJaVinculadoAUsuarioException
import com.crative.studio_api.usuario.exception.TelefoneObrigatorioParaProfessorException
import com.crative.studio_api.usuario.repository.ConviteUsuarioRepository
import com.crative.studio_api.usuario.repository.UsuarioRepository
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
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ConviteUsuarioServiceTest {

    @Mock lateinit var repository: ConviteUsuarioRepository
    @Mock lateinit var usuarioRepository: UsuarioRepository
    @Mock lateinit var usuarioService: UsuarioService
    @Mock lateinit var professorService: ProfessorService
    @Mock lateinit var professorRepository: ProfessorRepository

    private lateinit var service: ConviteUsuarioService

    @BeforeEach
    fun setUp() {
        service = ConviteUsuarioService(
            repository, usuarioRepository, usuarioService, professorService, professorRepository,
            frontendUrl = "http://localhost:4200",
            validadeHoras = 8
        )
    }

    private fun convite(
        email: String = "ana@studio.com",
        role: RoleType = RoleType.SECRETARIA,
        token: String = "token-123",
        status: StatusConvite = StatusConvite.PENDENTE,
        expiraEm: LocalDateTime = LocalDateTime.now().plusHours(8)
    ) = ConviteUsuarioEntity(
        email = email, role = role, token = token, status = status, expiraEm = expiraEm
    )

    private fun usuario(
        nome: String = "Ana",
        email: String = "ana@studio.com",
        role: RoleType = RoleType.SECRETARIA,
        professorId: UUID? = null
    ) = UsuarioEntity(nome = nome, email = email, senhaHash = "hash", role = role, professorId = professorId)

    // ---------- criar ----------

    @Test
    fun deve_criar_convite_pendente_com_link_contendo_o_token() {
        given(usuarioRepository.findByEmail("ana@studio.com")).willReturn(null)
        given(repository.findByEmailAndStatus("ana@studio.com", StatusConvite.PENDENTE)).willReturn(emptyList())
        given(repository.save(any<ConviteUsuarioEntity>())).willAnswer { it.arguments[0] }

        val antes = LocalDateTime.now().plusHours(8)
        val resultado = service.criar("ana@studio.com", RoleType.SECRETARIA)
        val depois = LocalDateTime.now().plusHours(8)

        assertEquals(StatusConvite.PENDENTE, resultado.convite.status)
        assertTrue(!resultado.convite.expiraEm.isBefore(antes) && !resultado.convite.expiraEm.isAfter(depois))
        assertEquals(
            "http://localhost:4200/completar-cadastro?token=${resultado.convite.token}",
            resultado.link
        )
    }

    /** O convite de professor não carrega ficha: ela nasce no completar. */
    @Test
    fun convite_de_professor_nasce_sem_vinculo_e_sem_tocar_no_cadastro_de_professor() {
        given(usuarioRepository.findByEmail("prof@studio.com")).willReturn(null)
        given(repository.findByEmailAndStatus("prof@studio.com", StatusConvite.PENDENTE)).willReturn(emptyList())
        given(repository.save(any<ConviteUsuarioEntity>())).willAnswer { it.arguments[0] }

        val resultado = service.criar("prof@studio.com", RoleType.PROFESSOR)

        assertEquals(RoleType.PROFESSOR, resultado.convite.role)
        assertNull(resultado.convite.professorId)
        verify(professorService, never()).cadastrar(any(), any(), anyOrNull())
    }

    @Test
    fun deve_recusar_convite_para_email_que_ja_tem_usuario() {
        given(usuarioRepository.findByEmail("ana@studio.com")).willReturn(usuario())

        assertThrows(EmailJaExisteNaBaseException::class.java) {
            service.criar("ana@studio.com", RoleType.SECRETARIA)
        }
        verify(repository, never()).save(any<ConviteUsuarioEntity>())
    }

    @Test
    fun novo_convite_expira_o_pendente_anterior_do_mesmo_email() {
        val anterior = convite(token = "token-antigo")
        given(usuarioRepository.findByEmail("ana@studio.com")).willReturn(null)
        given(repository.findByEmailAndStatus("ana@studio.com", StatusConvite.PENDENTE)).willReturn(listOf(anterior))
        given(repository.save(any<ConviteUsuarioEntity>())).willAnswer { it.arguments[0] }

        service.criar("ana@studio.com", RoleType.SECRETARIA)

        // dois saves: o anterior invalidado e o convite novo
        assertEquals(StatusConvite.EXPIRADO, anterior.status)
        verify(repository, times(2)).save(any<ConviteUsuarioEntity>())
    }

    // ---------- buscarPorToken ----------

    @Test
    fun deve_recusar_token_inexistente() {
        given(repository.findByToken("nao-existe")).willReturn(null)

        assertThrows(ConviteNaoEncontradoException::class.java) { service.buscarPorToken("nao-existe") }
    }

    @Test
    fun deve_recusar_convite_ja_utilizado() {
        given(repository.findByToken("token-123")).willReturn(convite(status = StatusConvite.UTILIZADO))

        assertThrows(ConviteInvalidoException::class.java) { service.buscarPorToken("token-123") }
    }

    /** Sem gravar EXPIRADO: a exceção derrubaria a transação e o UPDATE junto. */
    @Test
    fun deve_recusar_convite_vencido_sem_escrever_no_banco() {
        val vencido = convite(expiraEm = LocalDateTime.now().minusMinutes(1))
        given(repository.findByToken("token-123")).willReturn(vencido)

        assertThrows(ConviteInvalidoException::class.java) { service.buscarPorToken("token-123") }

        verify(repository, never()).save(any<ConviteUsuarioEntity>())
    }

    // ---------- completar ----------

    @Test
    fun deve_criar_usuario_com_os_dados_do_convite_e_marcar_utilizado() {
        val pendente = convite()
        given(repository.findByToken("token-123")).willReturn(pendente)
        given(
            usuarioService.cadastrarUsuario(
                eq("Ana Souza"), eq("ana@studio.com"), eq("senhaSegura1"), eq(RoleType.SECRETARIA), eq(null)
            )
        ).willReturn(usuario(nome = "Ana Souza"))

        val criado = service.completar("token-123", CompletarCadastroRequest("Ana Souza", "senhaSegura1"))

        assertEquals("ana@studio.com", criado.email)
        assertEquals(StatusConvite.UTILIZADO, pendente.status)
        verify(professorService, never()).cadastrar(any(), any(), anyOrNull())
        verify(repository).save(pendente)
    }

    @Test
    fun convite_de_professor_cria_a_ficha_com_os_dados_informados_pela_pessoa() {
        val professorId = UUID.randomUUID()
        given(repository.findByToken("token-123")).willReturn(convite(email = "prof@studio.com", role = RoleType.PROFESSOR))
        given(professorRepository.findByNomeAndTelefone("Marina", "11999990000")).willReturn(null)
        given(professorService.cadastrar("Marina", "11999990000", "Ballet"))
            .willReturn(ProfessorEntity(id = professorId, nome = "Marina", telefone = "11999990000", especialidade = "Ballet"))
        given(
            usuarioService.cadastrarUsuario(
                eq("Marina"), eq("prof@studio.com"), eq("senhaSegura1"), eq(RoleType.PROFESSOR), eq(professorId)
            )
        ).willReturn(usuario(nome = "Marina", email = "prof@studio.com", role = RoleType.PROFESSOR, professorId = professorId))

        val criado = service.completar(
            "token-123",
            CompletarCadastroRequest("Marina", "senhaSegura1", telefone = "11999990000", especialidade = "Ballet")
        )

        assertEquals(professorId, criado.professorId)
    }

    @Test
    fun deve_recusar_completar_convite_de_professor_sem_telefone() {
        given(repository.findByToken("token-123")).willReturn(convite(role = RoleType.PROFESSOR))

        assertThrows(TelefoneObrigatorioParaProfessorException::class.java) {
            service.completar("token-123", CompletarCadastroRequest("Marina", "senhaSegura1", telefone = "  "))
        }
        verify(usuarioService, never()).cadastrarUsuario(any(), any(), any(), any(), anyOrNull())
    }

    /** Professor que já dava aula e só agora ganha login: reaproveita a ficha, não cria outra. */
    @Test
    fun deve_reaproveitar_ficha_de_professor_com_mesmo_nome_e_telefone() {
        val professorId = UUID.randomUUID()
        given(repository.findByToken("token-123")).willReturn(convite(email = "prof@studio.com", role = RoleType.PROFESSOR))
        given(professorRepository.findByNomeAndTelefone("Marina", "11999990000"))
            .willReturn(ProfessorEntity(id = professorId, nome = "Marina", telefone = "11999990000", especialidade = null))
        given(usuarioRepository.findByProfessorId(professorId)).willReturn(null)
        given(
            usuarioService.cadastrarUsuario(
                eq("Marina"), eq("prof@studio.com"), eq("senhaSegura1"), eq(RoleType.PROFESSOR), eq(professorId)
            )
        ).willReturn(usuario(nome = "Marina", email = "prof@studio.com", role = RoleType.PROFESSOR, professorId = professorId))

        val criado = service.completar(
            "token-123",
            CompletarCadastroRequest("Marina", "senhaSegura1", telefone = "11999990000")
        )

        assertEquals(professorId, criado.professorId)
        verify(professorService, never()).cadastrar(any(), any(), anyOrNull())
    }

    @Test
    fun nao_deve_reaproveitar_ficha_de_professor_que_ja_tem_acesso() {
        val professorId = UUID.randomUUID()
        given(repository.findByToken("token-123")).willReturn(convite(email = "prof@studio.com", role = RoleType.PROFESSOR))
        given(professorRepository.findByNomeAndTelefone("Marina", "11999990000"))
            .willReturn(ProfessorEntity(id = professorId, nome = "Marina", telefone = "11999990000", especialidade = null))
        given(usuarioRepository.findByProfessorId(professorId))
            .willReturn(usuario(email = "outra@studio.com", role = RoleType.PROFESSOR, professorId = professorId))

        assertThrows(ProfessorJaVinculadoAUsuarioException::class.java) {
            service.completar("token-123", CompletarCadastroRequest("Marina", "senhaSegura1", telefone = "11999990000"))
        }
        verify(usuarioService, never()).cadastrarUsuario(any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun nao_deve_criar_usuario_ao_completar_convite_invalido() {
        given(repository.findByToken("token-123")).willReturn(convite(status = StatusConvite.UTILIZADO))

        assertThrows(ConviteInvalidoException::class.java) {
            service.completar("token-123", CompletarCadastroRequest("Ana", "senhaSegura1"))
        }
        verify(usuarioService, never()).cadastrarUsuario(any(), any(), any(), any(), anyOrNull())
    }

    // --- listagem ---

    /**
     * O convite vencido continua gravado como PENDENTE; é a listagem que precisa mostrá-lo como
     * EXPIRADO, senão a tela promete que alguém ainda vai entrar por um link que já morreu.
     */
    @Test
    fun deve_reportar_convite_pendente_vencido_como_expirado() {
        val vencido = convite(token = "vencido", expiraEm = LocalDateTime.now().minusHours(1))
        given(repository.findAllByOrderByCriadoEmDesc()).willReturn(listOf(vencido))

        val resultado = service.listar(status = null)

        assertEquals(StatusConvite.EXPIRADO, resultado.single().status)
        // A coluna em si não é reescrita — só a leitura.
        assertEquals(StatusConvite.PENDENTE, vencido.status)
    }

    @Test
    fun deve_filtrar_por_status_efetivo_e_nao_pela_coluna() {
        val vencido = convite(token = "vencido", expiraEm = LocalDateTime.now().minusHours(1))
        val valido = convite(token = "valido", expiraEm = LocalDateTime.now().plusHours(1))
        given(repository.findAllByOrderByCriadoEmDesc()).willReturn(listOf(vencido, valido))

        assertEquals(listOf("vencido"), service.listar(StatusConvite.EXPIRADO).map { it.convite.token })
        assertEquals(listOf("valido"), service.listar(StatusConvite.PENDENTE).map { it.convite.token })
    }

    @Test
    fun deve_montar_o_link_de_cada_convite_da_listagem() {
        given(repository.findAllByOrderByCriadoEmDesc()).willReturn(listOf(convite(token = "abc")))

        assertEquals(
            "http://localhost:4200/completar-cadastro?token=abc",
            service.listar(status = null).single().link
        )
    }

    // --- revogação ---

    @Test
    fun deve_revogar_convite_pendente() {
        val pendente = convite()
        given(repository.findByToken("token-123")).willReturn(pendente)

        service.revogar("token-123")

        verify(repository).save(org.mockito.kotlin.check { assertEquals(StatusConvite.REVOGADO, it.status) })
    }

    /** Depois de revogado, o link tem de parar de funcionar — quem garante isso é validarUtilizavel. */
    @Test
    fun convite_revogado_nao_pode_mais_ser_usado() {
        given(repository.findByToken("token-123")).willReturn(convite(status = StatusConvite.REVOGADO))

        assertThrows(ConviteInvalidoException::class.java) { service.buscarPorToken("token-123") }
    }

    @Test
    fun nao_deve_revogar_convite_ja_utilizado() {
        given(repository.findByToken("token-123")).willReturn(convite(status = StatusConvite.UTILIZADO))

        assertThrows(ConviteInvalidoException::class.java) { service.revogar("token-123") }
        verify(repository, never()).save(any())
    }

    @Test
    fun deve_lancar_excecao_ao_revogar_token_inexistente() {
        given(repository.findByToken("nao-existe")).willReturn(null)

        assertThrows(ConviteNaoEncontradoException::class.java) { service.revogar("nao-existe") }
    }
}
