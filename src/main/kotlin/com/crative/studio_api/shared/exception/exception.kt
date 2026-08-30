package com.crative.studio_api.shared.exception

abstract class NaoEncontradoException(message: String) : RuntimeException(message)
abstract class RecursoJaExisteException(message: String) : RuntimeException(message)
abstract class RegraDeNegocioException(message: String) : RuntimeException(message)