CREATE TABLE responsavel (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aluno_id    UUID NOT NULL,
    nome        VARCHAR(150) NOT NULL,
    telefone    VARCHAR(20) NOT NULL,
    cpf         VARCHAR(14),
    parentesco  VARCHAR(50) NOT NULL,

    CONSTRAINT fk_responsavel_aluno
        FOREIGN KEY (aluno_id)
        REFERENCES aluno(id)
);
