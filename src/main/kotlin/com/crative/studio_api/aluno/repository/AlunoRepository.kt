package com.crative.studio_api.aluno.repository

import com.crative.studio_api.aluno.entity.AlunoEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface AlunoRepository : JpaRepository<AlunoEntity, UUID> {
    fun findAllByAtivoTrue(): List<AlunoEntity>
    fun countByAtivoTrue(): Int
    fun findByCpf(cpf: String): AlunoEntity?

    fun findAllByAtivoTrueAndNomeContainingIgnoreCase(nome: String): List<AlunoEntity>

    /**
     * "Qual é mesmo o aluno da Ana?" — a secretária tem em mãos o nome ou o telefone de quem
     * liga, não o do aluno.
     *
     * O `exists` (em vez de um join) evita a linha duplicada do aluno com dois responsáveis que
     * casem com o termo. CPF entra na busca porque é a única chave estável do responsável.
     */
    @Query(
        """
        select a from AlunoEntity a
        where a.ativo = true
          and exists (
            select 1 from ResponsavelEntity r
            where r.alunoId = a.id
              and (
                lower(r.nome) like lower(concat('%', :termo, '%'))
                or r.telefone like concat('%', :termo, '%')
                or r.cpf like concat('%', :termo, '%')
              )
          )
        """
    )
    fun buscarAtivosPorResponsavel(@Param("termo") termo: String): List<AlunoEntity>
}
