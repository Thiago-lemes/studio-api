package com.crative.studio_api.usuario.controller

import com.crative.studio_api.usuario.dto.request.LoginRequest
import com.crative.studio_api.usuario.dto.response.LoginResponse
import com.crative.studio_api.usuario.mapper.toLogadoResponse
import com.crative.studio_api.usuario.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest
    ): ResponseEntity<LoginResponse> {

        val resultado = authService.autenticar(
            request.email,
            request.senha
        )

        return ResponseEntity.ok(
            LoginResponse(
                token = resultado.token,
                usuario = resultado.usuario.toLogadoResponse()
            )
        )
    }
}