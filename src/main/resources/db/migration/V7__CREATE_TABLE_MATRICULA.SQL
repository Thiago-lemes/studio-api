CREATE TABLE matricula
(
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aluno_id            UUID           NOT NULL REFERENCES aluno (id),
    turma_id            UUID           NOT NULL REFERENCES turma (id),
    status              VARCHAR(20)    NOT NULL DEFAULT 'ATIVA',
    valor_mensalidade   NUMERIC(10, 2) NOT NULL,
    dia_vencimento      INTEGER        NOT NULL CHECK (dia_vencimento BETWEEN 1 AND 28),
    desconto_percentual NUMERIC(5, 2)  DEFAULT 0,
    data_inicio         DATE           NOT NULL,
    data_fim            DATE,
    criado_em           TIMESTAMP      NOT NULL DEFAULT now(),
    UNIQUE (aluno_id, turma_id)
);

CREATE INDEX idx_matricula_aluno ON matricula (aluno_id);
CREATE INDEX idx_matricula_turma ON matricula (turma_id);
