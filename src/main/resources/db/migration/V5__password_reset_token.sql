CREATE TABLE IF NOT EXISTS "password_reset_token" (
    "id"         BIGSERIAL PRIMARY KEY,
    "user_id"    BIGINT NOT NULL REFERENCES "user"("id"),
    "token_hash" VARCHAR(64) NOT NULL UNIQUE,
    "expires_on" VARCHAR(255) NOT NULL
);
