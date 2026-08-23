package com.crative.studio_api.usuario.controller

import com.crative.studio_api.usuario.dto.request.CriarUsuarioRequest
import com.crative.studio_api.usuario.dto.response.UsuarioCriadoResponse
import com.crative.studio_api.usuario.dto.response.UsuarioResponse
import com.crative.studio_api.usuario.mapper.toCriarUsuarioResponse
import com.crative.studio_api.usuario.mapper.toUsuarioResponse
import com.crative.studio_api.usuario.service.UsuarioService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/usuarios")
class UsuarioController(
    private val service: UsuarioService
) {

    @PostMapping
    fun criar(@Valid @RequestBody request: CriarUsuarioRequest): ResponseEntity<UsuarioCriadoResponse> {
        val usuarioCriado = service.cadastrarUsuario(
            nome = request.nome,
            email = request.email,
            senha = request.senha,
            role = request.role
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioCriado.toCriarUsuarioResponse())
    }

    @GetMapping("/{id}")
    fun buscarPorId(
        @PathVariable id: UUID
    ): ResponseEntity<UsuarioResponse> {

        val usuario = service.buscarPorId(id)

        return ResponseEntity.ok(
            usuario.toUsuarioResponse()
        )
    }

}