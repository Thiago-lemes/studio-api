package com.crative.studio_api.financeiro.exception

import com.crative.studio_api.shared.exception.NaoEncontradoException
import com.crative.studio_api.shared.exception.RecursoJaExisteException
import com.crative.studio_api.shared.exception.RegraDeNegocioException

class ContaPagarNaoEncontradaException(message: String) : NaoEncontradoException(message)
class ContaReceberNaoEncontradaException(message: String) : NaoEncontradoException(message)
class PagamentoNaoEncontradoException(message: String) : NaoEncontradoException(message)
class ContaJaQuitadaException(message: String) : RecursoJaExisteException(message)
class ValorInvalidoException(message: String) : RegraDeNegocioException(message)
class DescricaoObrigatoriaException(message: String) : RegraDeNegocioException(message)
class ReferenciaInvalidaException(message: String) : RegraDeNegocioException(message)
class PeriodoInvalidoException(message: String) : RegraDeNegocioException(message)
