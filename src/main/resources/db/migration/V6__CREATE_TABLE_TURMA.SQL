CREATE TABLE turma
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    modalidade        VARCHAR(100) NOT NULL,
    professor_id      UUID         NOT NULL REFERENCES professor(id),
    sala_id           UUID         NOT NULL REFERENCES sala(id),
    horario_inicio    TIME         NOT NULL,
    horario_fim       TIME         NOT NULL,
    capacidade_maxima INTEGER      NOT NULL,
    ativa             BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE turma_dia_semana
(
    turma_id   UUID        NOT NULL REFERENCES turma(id),
    dia_semana VARCHAR(3)  NOT NULL,
    PRIMARY KEY (turma_id, dia_semana)
);