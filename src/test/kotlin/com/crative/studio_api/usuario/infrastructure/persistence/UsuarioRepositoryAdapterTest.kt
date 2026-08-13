package com.crative.studio_api.usuario.infrastructure.persistence

import com.crative.studio_api.shared.AbstractIntegrationTest
import com.crative.studio_api.usuario.domain.RoleType
import com.crative.studio_api.usuario.domain.UsuarioDomain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

class UsuarioRepositoryAdapterTest(
    private val repository: UsuarioRepositoryAdapter
) : AbstractIntegrationTest() {

    @Test
    @Transactional
    fun deve_salvar_e_buscar_usuario_por_id() {
        val usuario = UsuarioDomain.criar(
            nome = "Ana Secretária",
            email = "ana@studio.com",
            senhaHash = "hash123",
            role = RoleType.SECRETARIA,
            professorId = null
        )

        val salvo = repository.salvar(usuario)
        val encontrado = repository.buscarPorId(salvo.id)

        assertNotNull(encontrado)
        assertEquals(usuario.email, encontrado?.email)
        assertEquals(RoleType.SECRETARIA, encontrado?.role)
        assertNull(encontrado?.professorId)
    }

    @Test
    @Transactional
    fun deve_buscar_usuario_por_email() {
        val usuario = UsuarioDomain.criar(
            nome = "Carlos Admin",
            email = "carlos@studio.com",
            senhaHash = "hash456",
            role = RoleType.ADMIN,
            professorId = null
        )
        repository.salvar(usuario)

        val encontrado = repository.buscarPorEmail("carlos@studio.com")

        assertNotNull(encontrado)
        assertEquals("Carlos Admin", encontrado?.nome)
    }

    @Test
    @Transactional
    fun deve_retornar_null_ao_buscar_email_inexistente() {
        val encontrado = repository.buscarPorEmail("naoexiste@studio.com")

        assertNull(encontrado)
    }

    @Test
    @Transactional
    fun deve_listar_apenas_usuarios_ativos() {
        val ativo = UsuarioDomain.criar(
            nome = "Usuário Ativo",
            email = "ativo@studio.com",
            senhaHash = "hash",
            role = RoleType.ADMIN,
            professorId = null
        )
        repository.salvar(ativo)

        val listaAtivos = repository.listarAtivos()

        assertEquals(1, listaAtivos.size)
        assertEquals("ativo@studio.com", listaAtivos.first().email)
    }
}