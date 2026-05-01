CREATE TABLE otp_verifications (
                                   id          BIGSERIAL       PRIMARY KEY,
                                   email       VARCHAR(255)    NOT NULL,
                                   otp         VARCHAR(6)      NOT NULL,
                                   expires_at  TIMESTAMP       NOT NULL,
                                   used        BOOLEAN         NOT NULL DEFAULT false,
                                   created_at  TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_otp_email      ON otp_verifications (email);
CREATE INDEX idx_otp_expires_at ON otp_verifications (expires_at);
