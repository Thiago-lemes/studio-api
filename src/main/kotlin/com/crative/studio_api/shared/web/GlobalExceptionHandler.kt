package com.crative.studio_api.shared.web

import com.crative.studio_api.usuario.exception.CredenciaisInvalidasException
import com.crative.studio_api.usuario.exception.EmailJaExisteNaBaseException
import com.crative.studio_api.usuario.exception.ProfessorNaoDeveSerCadastradoNesseFluxoException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(CredenciaisInvalidasException::class)
    fun handleCredenciaisInvalidas(ex: CredenciaisInvalidasException): ResponseEntity<ErroResponse> {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ErroResponse(ex.message ?: "Credenciais inválidas", HttpStatus.UNAUTHORIZED.value()))
    }

    @ExceptionHandler(EmailJaExisteNaBaseException::class)
    fun handleEmailJaExiste(ex: EmailJaExisteNaBaseException): ResponseEntity<ErroResponse> {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErroResponse(ex.message ?: "Email já cadastrado", HttpStatus.CONFLICT.value()))
    }

    @ExceptionHandler(ProfessorNaoDeveSerCadastradoNesseFluxoException::class)
    fun handleProfessorNaoPermitido(ex: ProfessorNaoDeveSerCadastradoNesseFluxoException): ResponseEntity<ErroResponse> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErroResponse(ex.message ?: "Operação não permitida", HttpStatus.BAD_REQUEST.value()))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidacao(ex: MethodArgumentNotValidException): ResponseEntity<ErroResponse> {
        val mensagem = ex.bindingResult.fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErroResponse(mensagem, HttpStatus.BAD_REQUEST.value()))
    }
}