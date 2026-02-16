UPDATE users
SET username = lower(username)
WHERE username <> lower(username);

ALTER TABLE users
    ADD CONSTRAINT users_username_format_chk
        CHECK (username ~ '^[a-z0-9._-]{3,50}$');

ALTER TABLE users
    ADD CONSTRAINT users_username_lowercase_chk
        CHECK (username = lower(username));
