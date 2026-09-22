package com.crative.studio_api.shared

import com.crative.studio_api.academico.matricula.entity.MatriculaEntity
import com.crative.studio_api.academico.matricula.repository.MatriculaRepository
import com.crative.studio_api.academico.sala.entity.SalaEntity
import com.crative.studio_api.academico.sala.repository.SalaRepository
import com.crative.studio_api.academico.turma.entity.TurmaEntity
import com.crative.studio_api.academico.turma.repository.TurmaRepository
import com.crative.studio_api.academico.turma.types.DiaSemanaType
import com.crative.studio_api.aluno.entity.AlunoEntity
import com.crative.studio_api.aluno.entity.ResponsavelEntity
import com.crative.studio_api.aluno.repository.AlunoRepository
import com.crative.studio_api.aluno.repository.ResponsavelRepository
import com.crative.studio_api.financeiro.repository.ContaPagarRepository
import com.crative.studio_api.financeiro.repository.ContaReceberRepository
import com.crative.studio_api.financeiro.repository.PagamentoRepository
import com.crative.studio_api.professor.entity.ProfessorEntity
import com.crative.studio_api.professor.repository.ProfessorRepository
import com.crative.studio_api.shared.security.JwtService
import com.crative.studio_api.usuario.entity.ConviteUsuarioEntity
import com.crative.studio_api.usuario.entity.RoleType
import com.crative.studio_api.usuario.entity.StatusConvite
import com.crative.studio_api.usuario.entity.UsuarioEntity
import com.crative.studio_api.usuario.repository.ConviteUsuarioRepository
import com.crative.studio_api.usuario.repository.UsuarioRepository
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/**
 * Base dos testes de integração: sobe o contexto inteiro contra o Postgres real do
 * Testcontainers, com as migrations do Flyway aplicadas.
 *
 * Exige Docker em execução. O contexto é cacheado pelo Spring entre as classes que herdam
 * daqui, então a suíte sobe um container e um contexto só.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresContainerConfiguration::class)
abstract class AbstractIntegrationTest {

    @Autowired protected lateinit var mockMvc: MockMvc
    @Autowired protected lateinit var jdbcTemplate: JdbcTemplate
    @Autowired protected lateinit var jwtService: JwtService
    @Autowired protected lateinit var passwordEncoder: PasswordEncoder

    @Autowired protected lateinit var usuarioRepository: UsuarioRepository
    @Autowired protected lateinit var conviteUsuarioRepository: ConviteUsuarioRepository
    @Autowired protected lateinit var professorRepository: ProfessorRepository
    @Autowired protected lateinit var alunoRepository: AlunoRepository
    @Autowired protected lateinit var responsavelRepository: ResponsavelRepository
    @Autowired protected lateinit var salaRepository: SalaRepository
    @Autowired protected lateinit var turmaRepository: TurmaRepository
    @Autowired protected lateinit var matriculaRepository: MatriculaRepository
    @Autowired protected lateinit var contaReceberRepository: ContaReceberRepository
    @Autowired protected lateinit var contaPagarRepository: ContaPagarRepository
    @Autowired protected lateinit var pagamentoRepository: PagamentoRepository

    /**
     * TRUNCATE em vez de @Transactional + rollback: os serviços têm @Transactional próprio e
     * queremos ver o que realmente foi commitado, incluindo o efeito das constraints UNIQUE
     * (que é justamente o que garante a idempotência das cobranças).
     */
    @BeforeEach
    fun limparBanco() {
        jdbcTemplate.execute(
            """
            TRUNCATE TABLE pagamento, conta_receber, conta_pagar, matricula,
                           turma_dia_semana, turma, sala, responsavel, aluno,
                           convite_usuario, usuario, professor
            RESTART IDENTITY CASCADE
            """.trimIndent()
        )
    }

    // ---------- fixtures ----------

    protected fun criarUsuario(
        email: String = "secretaria@studio.com",
        senha: String = "senha123",
        role: RoleType = RoleType.SECRETARIA,
        professorId: UUID? = null,
        ativo: Boolean = true
    ): UsuarioEntity = usuarioRepository.save(
        UsuarioEntity(
            nome = "Usuário $role",
            email = email,
            senhaHash = requireNotNull(passwordEncoder.encode(senha)),
            role = role,
            professorId = professorId,
            ativo = ativo
        )
    )

    /** `expiraEm` é parâmetro para o teste de convite vencido não precisar mexer no relógio. */
    protected fun criarConvite(
        email: String = "convidado@studio.com",
        role: RoleType = RoleType.SECRETARIA,
        professorId: UUID? = null,
        token: String = UUID.randomUUID().toString(),
        status: StatusConvite = StatusConvite.PENDENTE,
        expiraEm: LocalDateTime = LocalDateTime.now().plusHours(8)
    ): ConviteUsuarioEntity = conviteUsuarioRepository.save(
        ConviteUsuarioEntity(
            email = email,
            role = role,
            professorId = professorId,
            token = token,
            status = status,
            expiraEm = expiraEm
        )
    )

    /** Header `Authorization` pronto para um papel, sem depender do endpoint de login. */
    protected fun tokenDe(role: RoleType = RoleType.SECRETARIA, professorId: UUID? = null): String =
        "Bearer " + jwtService.gerarToken(UUID.randomUUID(), role, professorId)

    protected fun criarProfessor(
        nome: String = "Prof. Marina",
        telefone: String = "11999990000"
    ): ProfessorEntity = professorRepository.save(
        ProfessorEntity(nome = nome, telefone = telefone, especialidade = "Ballet")
    )

    protected fun criarSala(
        nome: String = "Sala 1",
        capacidade: Int = 20
    ): SalaEntity = salaRepository.save(SalaEntity(nome = nome, capacidade = capacidade))

    protected fun criarTurma(
        professorId: UUID = criarProfessor().id!!,
        salaId: UUID = criarSala().id!!,
        modalidade: String = "Ballet Infantil",
        diasSemana: Set<DiaSemanaType> = setOf(DiaSemanaType.SEG),
        horarioInicio: LocalTime = LocalTime.of(9, 0),
        horarioFim: LocalTime = LocalTime.of(10, 0),
        capacidadeMaxima: Int = 10,
        ativa: Boolean = true
    ): TurmaEntity = turmaRepository.save(
        TurmaEntity(
            modalidade = modalidade,
            professorId = professorId,
            salaId = salaId,
            diasSemana = diasSemana.toMutableSet(),
            horarioInicio = horarioInicio,
            horarioFim = horarioFim,
            capacidadeMaxima = capacidadeMaxima,
            ativa = ativa
        )
    )

    /** Por padrão nasce adulto, para não exigir responsável nas matrículas. */
    protected fun criarAluno(
        nome: String = "Joana",
        cpf: String? = "111.111.111-11",
        dataNascimento: LocalDate = LocalDate.now().minusYears(25),
        ativo: Boolean = true
    ): AlunoEntity = alunoRepository.save(
        AlunoEntity(
            nome = nome,
            telefone = "11988887777",
            dataNascimento = dataNascimento,
            cpf = cpf,
            ativo = ativo
        )
    )

    protected fun criarResponsavel(alunoId: UUID, nome: String = "Maria"): ResponsavelEntity =
        responsavelRepository.save(
            ResponsavelEntity(
                alunoId = alunoId,
                nome = nome,
                telefone = "11977776666",
                cpf = null,
                parentesco = "Mãe"
            )
        )

    protected fun criarMatricula(
        alunoId: UUID,
        turmaId: UUID,
        valorMensalidade: BigDecimal = BigDecimal("300.00"),
        diaVencimento: Int = 10,
        descontoPercentual: BigDecimal = BigDecimal.ZERO,
        dataInicio: LocalDate = LocalDate.now()
    ): MatriculaEntity = matriculaRepository.save(
        MatriculaEntity(
            alunoId = alunoId,
            turmaId = turmaId,
            valorMensalidade = valorMensalidade,
            diaVencimento = diaVencimento,
            descontoPercentual = descontoPercentual,
            dataInicio = dataInicio
        )
    )
}
