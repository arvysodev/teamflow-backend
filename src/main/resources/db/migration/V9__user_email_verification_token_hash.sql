ALTER TABLE users
    RENAME COLUMN email_verification_token TO email_verification_token_hash;

DROP INDEX IF EXISTS users_email_verification_token_uq;

CREATE UNIQUE INDEX users_email_verification_token_hash_uq
    ON users (email_verification_token_hash)
    WHERE email_verification_token_hash IS NOT NULL;
