package com.crative.studio_api.academico.exception

import com.crative.studio_api.shared.exception.AcessoNegadoException
import com.crative.studio_api.shared.exception.NaoEncontradoException
import com.crative.studio_api.shared.exception.RecursoJaExisteException
import com.crative.studio_api.shared.exception.RegraDeNegocioException

class CapacidadeObrigatoriaException(message: String) : RegraDeNegocioException(message)
class NomeSalaObrigatorioException(message: String) : RegraDeNegocioException(message)
class SalaJaCadastradaException(message: String) : RecursoJaExisteException(message)
class SalaException(message: String) : RegraDeNegocioException(message)

class ChoqueDeHorarioException(message: String) : RecursoJaExisteException(message)
class TurmaNaoEncontradaException(message: String) : NaoEncontradoException(message)
class SalaNaoEncontradaException(message: String) : NaoEncontradoException(message)
class TurmaAcessoNegadoException(message: String) : AcessoNegadoException(message)
