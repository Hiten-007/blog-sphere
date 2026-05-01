CREATE TABLE password_reset_tokens (
                                       id          BIGSERIAL       PRIMARY KEY,
                                       email       VARCHAR(255)    NOT NULL,
                                       token       VARCHAR(255)    NOT NULL UNIQUE,
                                       expires_at  TIMESTAMP       NOT NULL,
                                       used        BOOLEAN         NOT NULL DEFAULT false,
                                       created_at  TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_prt_token      ON password_reset_tokens (token);
CREATE INDEX idx_prt_email      ON password_reset_tokens (email);
CREATE INDEX idx_prt_expires_at ON password_reset_tokens (expires_at);
