package com.crative.studio_api.usuario.exception

import com.crative.studio_api.shared.exception.NaoEncontradoException
import com.crative.studio_api.shared.exception.RecursoJaExisteException
import com.crative.studio_api.shared.exception.RegraDeNegocioException

class CredenciaisInvalidasException(message: String) : RuntimeException(message)
class UsuarioNaoEncontradoException(message: String) : RuntimeException(message)

class EmailJaExisteNaBaseException(message: String) : RecursoJaExisteException(message)

// Estendem RegraDeNegocioException para o GlobalExceptionHandler responder 400: como
// RuntimeException pura, elas não casavam com nenhum @ExceptionHandler e viravam 500.
class ProfessorIdObrigatorioException(message: String) : RegraDeNegocioException(message)
class ProfessorIdNaoPermitidoException(message: String) : RegraDeNegocioException(message)

class ConviteNaoEncontradoException(message: String) : NaoEncontradoException(message)
class ConviteInvalidoException(message: String) : RegraDeNegocioException(message)
class TelefoneObrigatorioParaProfessorException(message: String) : RegraDeNegocioException(message)
class ProfessorJaVinculadoAUsuarioException(message: String) : RecursoJaExisteException(message)

/**
 * Guardas da inativação de acesso. As duas existem para o mesmo fim: impedir que a tela de gestão
 * de usuários deixe o estúdio sem ninguém capaz de administrar o sistema.
 */
class AutoInativacaoNaoPermitidaException(message: String) : RegraDeNegocioException(message)
class UltimoAdminAtivoException(message: String) : RegraDeNegocioException(message)

class SenhaAtualIncorretaException(message: String) : RegraDeNegocioException(message)
class NovaSenhaIgualAAtualException(message: String) : RegraDeNegocioException(message)
