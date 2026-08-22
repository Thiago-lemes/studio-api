package com.crative.studio_api.usuario.infrastructure.web.controller

import com.crative.studio_api.usuario.application.usecase.CriarUsuarioUseCase
import com.crative.studio_api.usuario.infrastructure.web.dto.request.CriarUsuarioRequest
import com.crative.studio_api.usuario.infrastructure.web.dto.response.CriarUsuarioResponse
import com.crative.studio_api.usuario.infrastructure.web.dto.response.toCriarUsuarioResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/usuarios")
class UsuarioController(
    private val criarUsuarioUseCase: CriarUsuarioUseCase,
) {

    @PostMapping
    fun criar(@Valid @RequestBody request: CriarUsuarioRequest): ResponseEntity<CriarUsuarioResponse> {
        val usuarioCriado = criarUsuarioUseCase.cadastrarUsuario(
            nome = request.nome,
            email = request.email,
            senha = request.senha,
            role = request.role
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioCriado.toCriarUsuarioResponse())
    }
}