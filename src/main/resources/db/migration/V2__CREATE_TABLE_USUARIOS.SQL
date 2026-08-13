CREATE TABLE usuario
(
    ID                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    NOME                   VARCHAR(150) NOT NULL,
    EMAIL                  VARCHAR(150) NOT NULL UNIQUE,
    SENHA                  VARCHAR(255) NOT NULL,
    ROLE                   VARCHAR(20)  NOT NULL,          -- ADMIN, SECRETARIA, PROFESSOR
    PROFESSOR_ID           UUID REFERENCES professor (id), -- preenchido só quando papel = PROFESSOR
    ATIVO                  BOOLEAN      NOT NULL DEFAULT TRUE,
    CRIADO_EM              TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT chk_usuario_professor CHECK (
        (ROLE = 'PROFESSOR' AND PROFESSOR_ID IS NOT NULL) OR
        (ROLE != 'PROFESSOR' AND PROFESSOR_ID IS NULL)
        )
);