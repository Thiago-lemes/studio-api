CREATE TABLE convite_usuario
(
    ID           UUID PRIMARY KEY  DEFAULT gen_random_uuid(),
    EMAIL        VARCHAR(150) NOT NULL,
    ROLE         VARCHAR(20)  NOT NULL, -- ADMIN, SECRETARIA, PROFESSOR
    -- Sem CHECK de vínculo com professor, ao contrário do chk_usuario_professor da V2: o convite
    -- carrega só o e-mail e o papel. A ficha do professor nasce no completar, com os dados que a
    -- própria pessoa informa, então aqui a coluna fica sempre nula.
    PROFESSOR_ID UUID REFERENCES professor (id),
    TOKEN        VARCHAR(255) NOT NULL UNIQUE,
    STATUS       VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE', -- PENDENTE, UTILIZADO, EXPIRADO
    EXPIRA_EM    TIMESTAMP    NOT NULL,
    CRIADO_EM    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_convite_email_status ON convite_usuario (EMAIL, STATUS);
CREATE INDEX idx_convite_token ON convite_usuario (TOKEN);
