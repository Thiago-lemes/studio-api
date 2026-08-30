CREATE TABLE sala
(
    id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    nome       VARCHAR(80) NOT NULL,
    capacidade INTEGER     NOT NULL,
    criado_em  TIMESTAMP   NOT NULL DEFAULT now()
);