CREATE TABLE IF NOT EXISTS "member" (
    "id"          BIGSERIAL PRIMARY KEY,
    "name"        VARCHAR(255) NOT NULL,
    "email"       VARCHAR(255) NOT NULL,
    "password"    VARCHAR(255) NOT NULL,
    "user_id"     BIGINT,
    "created_on"  VARCHAR(255),
    "created_by"  BIGINT,
    "modified_on" VARCHAR(255),
    "modified_by" BIGINT
);

CREATE TABLE IF NOT EXISTS "genre" (
    "id"          BIGSERIAL PRIMARY KEY,
    "name"        VARCHAR(255) NOT NULL,
    "user_id"     BIGINT NOT NULL,
    "created_on"  VARCHAR(255),
    "created_by"  BIGINT,
    "modified_on" VARCHAR(255),
    "modified_by" BIGINT
);

CREATE TABLE IF NOT EXISTS "lending" (
    "id"                   BIGSERIAL PRIMARY KEY,
    "book_id"              BIGINT NOT NULL REFERENCES "book"("id"),
    "member_id"            BIGINT NOT NULL REFERENCES "member"("id"),
    "user_id"              BIGINT NOT NULL,
    "lent_date"            VARCHAR(255) NOT NULL,
    "expected_return_date" VARCHAR(255),
    "actual_return_date"   VARCHAR(255),
    "status"               VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    "created_on"           VARCHAR(255),
    "created_by"           BIGINT,
    "modified_on"          VARCHAR(255),
    "modified_by"          BIGINT
);

CREATE TABLE IF NOT EXISTS "user_activity_log" (
    "id"          BIGSERIAL PRIMARY KEY,
    "user_id"     BIGINT NOT NULL,
    "user_name"   VARCHAR(255),
    "action"      VARCHAR(50) NOT NULL,
    "book_id"     BIGINT,
    "book_name"   VARCHAR(255),
    "member_id"   BIGINT,
    "member_name" VARCHAR(255),
    "created_on"  VARCHAR(255),
    "created_by"  BIGINT,
    "modified_on" VARCHAR(255),
    "modified_by" BIGINT
);

ALTER TABLE "book" ADD COLUMN IF NOT EXISTS "genre_id" BIGINT REFERENCES "genre"("id");
ALTER TABLE "book" ADD COLUMN IF NOT EXISTS "rating"   INTEGER;
ALTER TABLE "book" ADD COLUMN IF NOT EXISTS "status"   VARCHAR(50) DEFAULT 'OWNED';
