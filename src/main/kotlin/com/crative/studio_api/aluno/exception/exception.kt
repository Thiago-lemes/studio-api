package com.crative.studio_api.aluno.exception

class AlunoNaoEncontradoException(message: String) : RuntimeException(message)
class DataNascimentoFuturaException(message: String) : RuntimeException(message)
class CpfJaCadastradoException(message: String) : RuntimeException(message)
class CpfNaoPodeSerNull(message: String) : RuntimeException(message)