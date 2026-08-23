CREATE TABLE aluno
(
    id              UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    nome            VARCHAR(150) NOT NULL,
    telefone        VARCHAR(20),
    data_nascimento DATE         NOT NULL,
    cpf             VARCHAR(14)  UNIQUE,
    ativo           BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP    NOT NULL DEFAULT now()
);