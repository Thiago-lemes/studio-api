package com.crative.studio_api.academico.exception

import com.crative.studio_api.shared.exception.RecursoJaExisteException
import com.crative.studio_api.shared.exception.RegraDeNegocioException

class CapacidadeObrigatoriaException(message: String) : RegraDeNegocioException(message)
class NomeSalaObrigatorioException(message: String) : RegraDeNegocioException(message)
class SalaJaCadastradaException(message: String) : RecursoJaExisteException(message)
class SalaException(message: String) : RegraDeNegocioException(message)