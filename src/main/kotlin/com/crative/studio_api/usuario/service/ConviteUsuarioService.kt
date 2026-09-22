package com.crative.studio_api.usuario.service

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
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

/** Convite recém-criado junto do link que o ADMIN vai repassar enquanto não há envio de e-mail. */
data class ConviteCriado(
    val convite: ConviteUsuarioEntity,
    val link: String
)

/**
 * Convite na listagem, com o status **efetivo** já resolvido.
 *
 * A coluna `status` guarda PENDENTE até alguém mexer no convite, então um convite vencido continua
 * gravado como PENDENTE (ver o comentário em `validarUtilizavel`). Para a tela, os dois são coisas
 * diferentes — "esperando a pessoa entrar" e "morreu sem ser usado" —, e é [status] que responde isso.
 */
data class ConviteResumo(
    val convite: ConviteUsuarioEntity,
    val status: StatusConvite,
    val link: String
)

@Service
class ConviteUsuarioService(
    private val repository: ConviteUsuarioRepository,
    private val usuarioRepository: UsuarioRepository,
    private val usuarioService: UsuarioService,
    private val professorService: ProfessorService,
    private val professorRepository: ProfessorRepository,

    @Value("\${app.frontend-url}")
    private val frontendUrl: String,

    @Value("\${app.convite.validade-horas}")
    private val validadeHoras: Long
) {

    @Transactional
    fun criar(email: String, role: RoleType): ConviteCriado {
        // findByEmail (e não findByEmailAndAtivoTrue): a coluna EMAIL é UNIQUE, então um usuário
        // inativo também impede o convite — reabrir o acesso dele é reativação, não convite novo.
        usuarioRepository.findByEmail(email)?.let {
            throw EmailJaExisteNaBaseException("Já existe usuário com este e-mail")
        }

        // Reenviar convite é comportamento esperado, não erro: o anterior perde a validade.
        repository.findByEmailAndStatus(email, StatusConvite.PENDENTE).forEach {
            it.status = StatusConvite.EXPIRADO
            repository.save(it)
        }

        val convite = repository.save(
            ConviteUsuarioEntity(
                email = email,
                role = role,
                token = UUID.randomUUID().toString(),
                expiraEm = LocalDateTime.now().plusHours(validadeHoras)
            )
        )

        // O link é montado aqui, e não no controller: quando o notification-service entrar, só
        // esta linha muda de "devolver" para "publicar evento".
        return ConviteCriado(convite, montarLink(convite.token))
    }

    /**
     * O filtro é aplicado sobre o status efetivo, não sobre a coluna — `?status=EXPIRADO` precisa
     * trazer também os PENDENTE já vencidos, senão a tela mostraria como "aguardando" um convite
     * que ninguém mais consegue usar. O volume é de dezenas de linhas; filtrar em memória aqui
     * custa menos que espalhar a regra de vencimento numa query.
     */
    @Transactional(readOnly = true)
    fun listar(status: StatusConvite?): List<ConviteResumo> {
        return repository.findAllByOrderByCriadoEmDesc()
            .map { ConviteResumo(it, statusEfetivo(it), montarLink(it.token)) }
            .filter { status == null || it.status == status }
    }

    /**
     * Revogar é o caminho do convite enviado por engano: sem isto, só restava esperar o vencimento.
     * Grava REVOGADO em vez de apagar a linha — o rastro de quem convidou quem se perderia, e o
     * token continuaria livre para colidir. Como `validarUtilizavel` só aceita PENDENTE, o link
     * para de funcionar no mesmo instante.
     */
    @Transactional
    fun revogar(token: String) {
        val convite = repository.findByToken(token)
            ?: throw ConviteNaoEncontradoException("Convite não encontrado")

        if (convite.status == StatusConvite.UTILIZADO) {
            throw ConviteInvalidoException(
                "Este convite já foi utilizado. Para bloquear o acesso, inative o usuário."
            )
        }

        convite.status = StatusConvite.REVOGADO
        repository.save(convite)
    }

    @Transactional(readOnly = true)
    fun buscarPorToken(token: String): ConviteUsuarioEntity {
        val convite = repository.findByToken(token)
            ?: throw ConviteNaoEncontradoException("Convite não encontrado")

        validarUtilizavel(convite)
        return convite
    }

    /**
     * Transacional de ponta a ponta de propósito: se a criação do usuário falhar depois da ficha
     * do professor ter sido gravada, a ficha some junto e o convite continua PENDENTE — sem isso
     * sobraria professor órfão com convite ainda aberto.
     */
    @Transactional
    fun completar(token: String, request: CompletarCadastroRequest): UsuarioEntity {
        val convite = buscarPorToken(token)

        val professorId = if (convite.role == RoleType.PROFESSOR) {
            resolverProfessor(request.nome, request.telefone, request.especialidade)
        } else {
            null
        }

        // E-mail e role saem do convite — o corpo não os traz, então não há como completar o
        // cadastro com um e-mail diferente do que o ADMIN convidou.
        val usuario = usuarioService.cadastrarUsuario(
            nome = request.nome,
            email = convite.email,
            senha = request.senha,
            role = convite.role,
            professorId = professorId
        )

        convite.status = StatusConvite.UTILIZADO
        repository.save(convite)

        return usuario
    }

    /**
     * Nome + telefone iguais aos de uma ficha existente **reaproveitam** essa ficha em vez de
     * recusar: é o caso do professor que já dava aula e só agora está ganhando login. Se aquela
     * ficha já tiver um usuário, aí sim é conflito — senão dois logins apontariam para o mesmo
     * professor.
     */
    private fun resolverProfessor(nome: String, telefone: String?, especialidade: String?): UUID {
        val telefoneInformado = telefone?.takeIf { it.isNotBlank() }
            ?: throw TelefoneObrigatorioParaProfessorException("telefone: obrigatório para cadastro de professor")

        val existente = professorRepository.findByNomeAndTelefone(nome, telefoneInformado)
            ?: return requireNotNull(professorService.cadastrar(nome, telefoneInformado, especialidade).id)

        val professorId = requireNotNull(existente.id)
        usuarioRepository.findByProfessorId(professorId)?.let {
            throw ProfessorJaVinculadoAUsuarioException(
                "Já existe um acesso vinculado a este professor. Fale com o administrador."
            )
        }

        return professorId
    }

    private fun validarUtilizavel(convite: ConviteUsuarioEntity) {
        if (convite.status != StatusConvite.PENDENTE) {
            throw ConviteInvalidoException("Este convite já foi utilizado ou não é mais válido")
        }
        // Não grava EXPIRADO aqui: a exceção derruba a transação e o UPDATE voltaria atrás junto.
        // "PENDENTE e vencido" é derivável de expira_em; quem de fato muda o status é o reenvio.
        if (convite.expiraEm.isBefore(LocalDateTime.now())) {
            throw ConviteInvalidoException("Este convite expirou")
        }
    }

    private fun statusEfetivo(convite: ConviteUsuarioEntity): StatusConvite =
        if (convite.status == StatusConvite.PENDENTE && convite.expiraEm.isBefore(LocalDateTime.now())) {
            StatusConvite.EXPIRADO
        } else {
            convite.status
        }

    private fun montarLink(token: String) =
        "${frontendUrl.trimEnd('/')}/completar-cadastro?token=$token"
}
