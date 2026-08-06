CREATE TABLE IF NOT EXISTS "user_preference" (
    "id"                          BIGSERIAL PRIMARY KEY,
    "user_id"                     BIGINT NOT NULL UNIQUE REFERENCES "user"("id"),
    "library_name"                VARCHAR(255),
    "owner_name"                  VARCHAR(255),
    "description"                 TEXT,
    "default_loan_duration_days"  INTEGER NOT NULL DEFAULT 30,
    "date_format"                 VARCHAR(50) NOT NULL DEFAULT 'DD MMM YYYY',
    "language"                    VARCHAR(10) NOT NULL DEFAULT 'en',
    "created_on"                  VARCHAR(255),
    "created_by"                  BIGINT,
    "modified_on"                 VARCHAR(255),
    "modified_by"                 BIGINT
);

CREATE TABLE IF NOT EXISTS "export_job" (
    "id"           BIGSERIAL PRIMARY KEY,
    "user_id"      BIGINT NOT NULL REFERENCES "user"("id"),
    "status"       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    "file_path"    VARCHAR(500),
    "error"        VARCHAR(500),
    "created_on"   VARCHAR(255) NOT NULL,
    "completed_on" VARCHAR(255),
    "expires_on"   VARCHAR(255)
);
