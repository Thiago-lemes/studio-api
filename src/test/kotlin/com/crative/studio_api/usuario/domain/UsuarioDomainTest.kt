package com.crative.studio_api.usuario.domain

import com.crative.studio_api.usuario.domain.exception.ProfessorIdNaoPermitidoException
import com.crative.studio_api.usuario.domain.exception.ProfessorIdObrigatorioException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class UsuarioDomainTest {
    @Test
    fun deve_lancar_excecao_ao_criar_usuario_PROFESSOR_sem_professorId() {
        assertThrows<ProfessorIdObrigatorioException> {
            UsuarioDomain.criar(
                nome = "João",
                email = "joao@studio.com",
                senhaHash = "hash",
                role = RoleType.PROFESSOR,
                professorId = null
            )
        }
    }

    @Test
    fun deve_lancar_excecao_ao_criar_usuario_ADMIN_com_professorId_preenchido() {
        assertThrows<ProfessorIdNaoPermitidoException> {
            UsuarioDomain.criar(
                nome = "Maria",
                email = "maria@studio.com",
                senhaHash = "hash",
                role = RoleType.ADMIN,
                professorId = UUID.randomUUID()
            )
        }
    }

    @Test
    fun deve_criar_usuario_PROFESSOR_com_professorId_valido() {
        val usuarioDomain = UsuarioDomain.criar(
            nome = "João",
            email = "joao@studio.com",
            senhaHash = "hash",
            role = RoleType.PROFESSOR,
            professorId = UUID.randomUUID()
        )

        Assertions.assertEquals(RoleType.PROFESSOR, usuarioDomain.role)
        assertTrue(usuarioDomain.ativo)
    }

    @Test
    fun deve_lancar_excecao_ao_criar_usuario_SECRETARIA_com_professorId_preenchido() {
        assertThrows(ProfessorIdNaoPermitidoException::class.java) {
            UsuarioDomain.criar(
                nome = "Ana",
                email = "ana@studio.com",
                senhaHash = "hash",
                role = RoleType.SECRETARIA,
                professorId = UUID.randomUUID()
            )
        }
    }
}