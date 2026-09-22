package com.crative.studio_api.dashboard.dto

import com.crative.studio_api.academico.turma.types.DiaSemanaType
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * Uma ocorrência concreta de uma turma numa data — o que o calendário do front vinha derivando
 * sozinho a partir de `GET /turmas`.
 *
 * Não existe tabela por trás disto: a agenda é a expansão da recorrência semanal da turma dentro
 * do intervalo pedido. Como consequência, **não há noção de aula cancelada, feriado ou reposição**
 * — toda semana no intervalo produz as mesmas ocorrências. Marcar exceções exige uma entidade de
 * aula de verdade, que ainda não existe.
 */
data class AgendaAulaResponse(
    val data: LocalDate,
    val diaSemana: DiaSemanaType,
    val horarioInicio: LocalTime,
    val horarioFim: LocalTime,

    val turmaId: UUID,
    val modalidade: String,
    val professorId: UUID,
    val professorNome: String,
    val salaId: UUID,
    val salaNome: String,

    val capacidadeMaxima: Int,
    val alunosMatriculados: Int,
    val vagasDisponiveis: Int
)
