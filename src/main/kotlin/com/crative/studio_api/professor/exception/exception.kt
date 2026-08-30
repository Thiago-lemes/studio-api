package com.crative.studio_api.professor.exception

import com.crative.studio_api.shared.exception.RegraDeNegocioException
import com.crative.studio_api.shared.exception.NaoEncontradoException
import com.crative.studio_api.shared.exception.RecursoJaExisteException

class NomeObrigatorioException(message: String) : RegraDeNegocioException(message)
class ProfessorNaoEncontradoException(message: String) : NaoEncontradoException(message)
class ProfessorJaCadastradoException(message: String) : RecursoJaExisteException(message)