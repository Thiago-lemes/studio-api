package com.crative.studio_api.shared.web

import com.crative.studio_api.shared.exception.AcessoNegadoException
import com.crative.studio_api.shared.exception.NaoEncontradoException
import com.crative.studio_api.shared.exception.RecursoJaExisteException
import com.crative.studio_api.shared.exception.RegraDeNegocioException
import com.crative.studio_api.usuario.exception.CredenciaisInvalidasException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NaoEncontradoException::class)
    fun handleNaoEncontrado(ex: NaoEncontradoException): ResponseEntity<ErroResponse> =
        responder(HttpStatus.NOT_FOUND, ex.message)

    @ExceptionHandler(RecursoJaExisteException::class)
    fun handleJaExiste(ex: RecursoJaExisteException): ResponseEntity<ErroResponse> =
        responder(HttpStatus.CONFLICT, ex.message)

    @ExceptionHandler(RegraDeNegocioException::class)
    fun handleRegraDeNegocio(ex: RegraDeNegocioException): ResponseEntity<ErroResponse> =
        responder(HttpStatus.BAD_REQUEST, ex.message)

    @ExceptionHandler(AcessoNegadoException::class)
    fun handleAcessoNegado(ex: AcessoNegadoException): ResponseEntity<ErroResponse> =
        responder(HttpStatus.FORBIDDEN, ex.message)

    @ExceptionHandler(CredenciaisInvalidasException::class)
    fun handleCredenciaisInvalidas(ex: CredenciaisInvalidasException): ResponseEntity<ErroResponse> =
        responder(HttpStatus.UNAUTHORIZED, ex.message)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidacao(ex: MethodArgumentNotValidException): ResponseEntity<ErroResponse> {
        val mensagem = ex.bindingResult.fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return responder(HttpStatus.BAD_REQUEST, mensagem)
    }

    private fun responder(status: HttpStatus, mensagem: String?): ResponseEntity<ErroResponse> {
        return ResponseEntity.status(status).body(ErroResponse(mensagem ?: "Erro inesperado", status.value()))
    }
}