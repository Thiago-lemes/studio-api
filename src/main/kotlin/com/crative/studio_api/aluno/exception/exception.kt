package com.crative.studio_api.aluno.exception

import com.crative.studio_api.shared.exception.NaoEncontradoException
import com.crative.studio_api.shared.exception.RecursoJaExisteException
import com.crative.studio_api.shared.exception.RegraDeNegocioException

class AlunoNaoEncontradoException(message: String) : NaoEncontradoException(message)
class ResponsavelNaoEncontradoException(message: String) : NaoEncontradoException(message)
class CpfJaCadastradoException(message: String) : RecursoJaExisteException(message)
class CpfNaoPodeSerNull(message: String) : RegraDeNegocioException(message)
class DataNascimentoFuturaException(message: String) : RegraDeNegocioException(message)
