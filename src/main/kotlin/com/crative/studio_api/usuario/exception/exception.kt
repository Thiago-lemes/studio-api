package com.crative.studio_api.usuario.exception

import com.crative.studio_api.shared.exception.RecursoJaExisteException
import com.crative.studio_api.shared.exception.RegraDeNegocioException

class ProfessorIdObrigatorioException(message: String) : RuntimeException(message)
class ProfessorIdNaoPermitidoException(message: String) : RuntimeException(message)
class CredenciaisInvalidasException(message: String) : RuntimeException(message)
class UsuarioNaoEncontradoException(message: String) : RuntimeException(message)

class EmailJaExisteNaBaseException(message: String) : RecursoJaExisteException(message)
class ProfessorNaoDeveSerCadastradoNesseFluxoException(message: String) : RegraDeNegocioException(message)
