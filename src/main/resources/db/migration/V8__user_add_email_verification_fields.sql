ALTER TABLE users
    ADD COLUMN email_verified_at TIMESTAMP NULL,
    ADD COLUMN email_verification_token VARCHAR(255) NULL,
    ADD COLUMN email_verification_token_expires_at TIMESTAMP NULL;

CREATE UNIQUE INDEX users_email_verification_token_uq
    ON users (email_verification_token)
    WHERE email_verification_token IS NOT NULL;
