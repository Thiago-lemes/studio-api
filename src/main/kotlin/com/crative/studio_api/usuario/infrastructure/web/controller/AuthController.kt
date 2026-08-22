package com.crative.studio_api.usuario.infrastructure.web.controller

import com.crative.studio_api.usuario.application.usecase.AutenticarUsuarioUseCase
import com.crative.studio_api.usuario.infrastructure.web.dto.request.LoginRequest
import com.crative.studio_api.usuario.infrastructure.web.dto.response.LoginResponse
import com.crative.studio_api.usuario.infrastructure.web.dto.response.toLogadoResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val autenticarUsuarioUseCase: AutenticarUsuarioUseCase
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val resultado = autenticarUsuarioUseCase.autenticar(request.email, request.senha)

        val response = LoginResponse(
            token = resultado.token,
            usuario = resultado.usuario.toLogadoResponse()
        )

        return ResponseEntity.ok(response)
    }
}