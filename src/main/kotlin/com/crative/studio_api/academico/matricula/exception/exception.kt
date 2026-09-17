package com.crative.studio_api.academico.matricula.exception

import com.crative.studio_api.shared.exception.NaoEncontradoException
import com.crative.studio_api.shared.exception.RecursoJaExisteException
import com.crative.studio_api.shared.exception.RegraDeNegocioException

class MatriculaNaoEncontradaException(message: String) : NaoEncontradoException(message)
class AlunoJaMatriculadoNaTurmaException(message: String) : RecursoJaExisteException(message)
class TurmaSemVagaException(message: String) : RegraDeNegocioException(message)
class TurmaInativaException(message: String) : RegraDeNegocioException(message)
class AlunoInativoException(message: String) : RegraDeNegocioException(message)
class ResponsavelObrigatorioException(message: String) : RegraDeNegocioException(message)
class DadosFinanceirosInvalidosException(message: String) : RegraDeNegocioException(message)
class TransicaoDeStatusInvalidaException(message: String) : RegraDeNegocioException(message)
