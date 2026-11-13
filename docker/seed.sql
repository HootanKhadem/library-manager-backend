-- Seed data for the diagnostics/testing stack (docker-compose.test.yml).
-- Assumes a fresh DB: the ADMIN user (id=1) was already created by app startup.
-- /api/* routes require Role.USER (ADMIN is excluded), so a second, USER-role
-- account is seeded here and owns all the library data.
-- Login: POST /auth/login {"email":"testuser@example.com","password":"Test1234"}

INSERT INTO "user" ("username", "password", "email", "role", "salt", "created_on", "created_by") VALUES
    ('testuser', 'ba0e5578616fd10ed14268e6aa6941311443a17d876ea643495e1267bcbe8a28', 'testuser@example.com', 'USER', 'dGVzdHNhbHQxMjM0NTY3ODkwMTI=', '2026-01-01', 1);

INSERT INTO "genre" ("name", "user_id", "created_on", "created_by") VALUES
    ('Fiction', 2, '2026-01-01', 2),
    ('Science', 2, '2026-01-01', 2),
    ('History', 2, '2026-01-01', 2);

INSERT INTO "author" ("name", "image", "user_id", "created_on", "created_by") VALUES
    ('George Orwell', '', 2, '2026-01-01', 2),
    ('Carl Sagan', '', 2, '2026-01-01', 2),
    ('Yuval Noah Harari', '', 2, '2026-01-01', 2);

INSERT INTO "book" ("name", "author_id", "pages", "isbn", "published_date", "publisher", "quantity", "genre_id", "rating", "status", "user_id", "created_on", "created_by") VALUES
    ('1984', 1, 328, '9780451524935', '1949-06-08', 'Secker & Warburg', 3, 1, 5, 'OWNED', 2, '2026-01-02', 2),
    ('Animal Farm', 1, 112, '9780451526342', '1945-08-17', 'Secker & Warburg', 2, 1, 4, 'OWNED', 2, '2026-01-02', 2),
    ('Cosmos', 2, 365, '9780345539434', '1980-01-01', 'Random House', 1, 2, 5, 'OWNED', 2, '2026-01-02', 2),
    ('Sapiens', 3, 443, '9780062316097', '2011-01-01', 'Harvill Secker', 2, 3, 5, 'OWNED', 2, '2026-01-02', 2);

INSERT INTO "member" ("name", "email", "password", "user_id", "created_on", "created_by") VALUES
    ('Alice Example', 'alice@example.com', 'n/a', 2, '2026-01-03', 2),
    ('Bob Example', 'bob@example.com', 'n/a', 2, '2026-01-03', 2);

-- Active loan: Alice currently has "1984".
INSERT INTO "lending" ("book_id", "member_id", "user_id", "lent_date", "expected_return_date", "status", "created_on", "created_by") VALUES
    (1, 1, 2, '2026-01-10', '2026-02-10', 'ACTIVE', '2026-01-10', 2);

-- Completed loan: Bob already returned "Cosmos".
INSERT INTO "lending" ("book_id", "member_id", "user_id", "lent_date", "expected_return_date", "actual_return_date", "status", "created_on", "created_by") VALUES
    (3, 2, 2, '2025-12-01', '2025-12-31', '2025-12-28', 'RETURNED', '2025-12-01', 2);

INSERT INTO "user_activity_log" ("user_id", "user_name", "action", "book_id", "book_name", "member_id", "member_name", "created_on", "created_by") VALUES
    (2, 'testuser', 'LENT', 1, '1984', 1, 'Alice Example', '2026-01-10', 2),
    (2, 'testuser', 'LENT', 3, 'Cosmos', 2, 'Bob Example', '2025-12-01', 2),
    (2, 'testuser', 'RETURNED', 3, 'Cosmos', 2, 'Bob Example', '2025-12-28', 2);

INSERT INTO "user_preference" ("user_id", "library_name", "owner_name", "default_loan_duration_days", "created_on", "created_by") VALUES
    (2, 'Diagnostics Test Library', 'testuser', 30, '2026-01-01', 2);
