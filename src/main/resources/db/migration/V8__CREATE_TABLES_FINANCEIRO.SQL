CREATE TABLE conta_pagar
(
    id         UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    descricao  VARCHAR(255)   NOT NULL,
    categoria  VARCHAR(20)    NOT NULL,
    valor      NUMERIC(10, 2) NOT NULL,
    vencimento DATE           NOT NULL,
    status     VARCHAR(20)    NOT NULL DEFAULT 'PENDENTE',
    pago_em    TIMESTAMP,
    criado_em  TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_conta_pagar_status ON conta_pagar (status);
CREATE INDEX idx_conta_pagar_vencimento ON conta_pagar (vencimento);

CREATE TABLE conta_receber
(
    id           UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    matricula_id UUID           NOT NULL REFERENCES matricula (id),
    referencia   VARCHAR(7)     NOT NULL,
    valor        NUMERIC(10, 2) NOT NULL,
    vencimento   DATE           NOT NULL,
    status       VARCHAR(20)    NOT NULL DEFAULT 'PENDENTE',
    criado_em    TIMESTAMP      NOT NULL DEFAULT now(),
    UNIQUE (matricula_id, referencia)
);

CREATE INDEX idx_conta_receber_matricula ON conta_receber (matricula_id);
CREATE INDEX idx_conta_receber_status ON conta_receber (status);
CREATE INDEX idx_conta_receber_vencimento ON conta_receber (vencimento);

CREATE TABLE pagamento
(
    id               UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    conta_receber_id UUID           NOT NULL REFERENCES conta_receber (id),
    valor_pago       NUMERIC(10, 2) NOT NULL,
    forma_pagamento  VARCHAR(20)    NOT NULL,
    pago_em          TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_pagamento_conta_receber ON pagamento (conta_receber_id);
CREATE INDEX idx_pagamento_pago_em ON pagamento (pago_em);
