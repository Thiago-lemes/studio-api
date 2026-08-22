package com.crative.studio_api.usuario.exception

class ProfessorIdObrigatorioException(message: String) : RuntimeException(message)
class ProfessorIdNaoPermitidoException(message: String) : RuntimeException(message)
class CredenciaisInvalidasException(message: String) : RuntimeException(message)
class EmailJaExisteNaBaseException(message: String) : RuntimeException(message)
class ProfessorNaoDeveSerCadastradoNesseFluxoException(message: String) : RuntimeException(message)
class UsuarioNaoEncontradoException(message: String) : RuntimeException(message)

