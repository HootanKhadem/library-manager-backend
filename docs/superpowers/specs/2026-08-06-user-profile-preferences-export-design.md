# User Profile: Preferences & Data Export — API Design

Date: 2026-08-06
Status: Approved

## Context

The frontend Preferences page (see mockup provided by user) has two functional areas:

1. **General Settings / Language** — per-user library metadata and display preferences (library name, owner name, default loan duration, date format, description, language).
2. **Export & Backup** — the page shows CSV / JSON / Print Catalogue buttons, but only CSV backup is in scope. JSON export and Print Catalogue are dropped from this design (frontend can remove those buttons or leave them disabled — not a backend concern).

This project (`library-manager-backend`) is Kotlin/Ktor + Exposed on Postgres. All domain entities (`book`, `member`, `genre`, `lending`) are already scoped per-user via a `user_id` column, confirming the app is multi-tenant per user account — each user owns their own personal library. Preferences and exports follow the same per-user scoping.

No existing preferences table, no existing export/CSV functionality, no existing file-storage abstraction (no S3/cloud storage config found). Both parts are greenfield.

## Part 1 — User Data & Preferences

### Data model

New table, one row per user (lazy-created on first write; GET returns defaults if no row exists yet — avoids touching the registration flow):

```sql
CREATE TABLE IF NOT EXISTS "user_preference" (
    "user_id"                    BIGINT PRIMARY KEY REFERENCES "user"("id"),
    "library_name"               VARCHAR(255),
    "owner_name"                 VARCHAR(255),
    "description"                TEXT,
    "default_loan_duration_days" INTEGER NOT NULL DEFAULT 30,
    "date_format"                VARCHAR(50) NOT NULL DEFAULT 'DD MMM YYYY',
    "language"                   VARCHAR(10) NOT NULL DEFAULT 'en',
    "created_on"                 VARCHAR(255),
    "created_by"                 BIGINT,
    "modified_on"                VARCHAR(255),
    "modified_by"                BIGINT
);
```

`user_id` is both PK and FK — enforces the 1-1 relationship at the schema level, no surrogate id needed.

### DTO

```kotlin
@Serializable
data class UserPreferenceDTO(
    val libraryName: String? = null,
    val ownerName: String? = null,
    val description: String? = null,
    val defaultLoanDurationDays: Int = 30,
    val dateFormat: String = "DD MMM YYYY",
    val language: String = "en",
    val modifiedOn: String? = null
)
```

### Endpoints

Auth required (existing JWT/cookie auth). Caller's `userId` comes from the token — no id in the path, and a user can never read or write another user's preferences.

| Method | Path | Body | Response | Purpose |
|---|---|---|---|---|
| GET | `/users/me/preferences` | – | `200 UserPreferenceDTO` | Fetch current preferences (schema defaults if no row yet) |
| PUT | `/users/me/preferences` | `UserPreferenceDTO` | `200 UserPreferenceDTO` | Full replace — mirrors the form's "Save Changes" (whole-form submit, not partial-field editing) |

`PUT` is used instead of `PATCH` because the form has no per-field save — "Reset"/"Save Changes" implies submitting the whole block at once.

### Validation

- `defaultLoanDurationDays` must be > 0.
- `language` must be in the allowed set (`en`, `fa`).
- `dateFormat` must be in an allowed enum set (defined server-side, matching the frontend's dropdown options).
- Unknown/invalid values → `400`, not silently coerced or clamped.

### Errors

| Code | Cause |
|---|---|
| 401 | Missing/invalid auth token |
| 400 | Validation failure (bad enum value, non-positive duration) |

(No 404 — lazy-create covers the "no preferences yet" case.)

## Part 2 — Export Data (CSV Backup, Async)

### Scope

"Backup" = full export of everything the user's personal library owns: books, members, lendings, genres — not just the book catalogue. Delivered as CSV, one file per entity, zipped together (`export_<userId>_<timestamp>.zip` containing `books.csv`, `members.csv`, `lendings.csv`, `genres.csv`).

Run as an async job (not a synchronous single-request download): a background coroutine (fits the existing Ktor/coroutine stack, no new queue infra needed) pulls each entity via existing repositories, streams to CSV, zips, and writes the result to local disk. A DB row tracks job status and file path — chosen over storing CSV bytes in the DB to avoid bloating Postgres with binary blobs (this project has no existing cloud storage integration, so local disk + DB-tracked path is the simplest fit for its current deployment shape).

### Data model

```sql
CREATE TABLE IF NOT EXISTS "export_job" (
    "id"           BIGSERIAL PRIMARY KEY,
    "user_id"      BIGINT NOT NULL REFERENCES "user"("id"),
    "status"       VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, RUNNING, COMPLETED, FAILED
    "file_path"    VARCHAR(500),
    "error"        VARCHAR(500),
    "created_on"   VARCHAR(255) NOT NULL,
    "completed_on" VARCHAR(255),
    "expires_on"   VARCHAR(255)
);
```

### DTO

```kotlin
enum class ExportJobStatus { PENDING, RUNNING, COMPLETED, FAILED }

@Serializable
data class ExportJobDTO(
    val id: Long,
    val status: ExportJobStatus,
    val createdOn: String,
    val completedOn: String? = null,
    val error: String? = null
)
```

### Endpoints

| Method | Path | Body | Response | Purpose |
|---|---|---|---|---|
| POST | `/users/me/exports` | – | `202 ExportJobDTO` (status=PENDING) | Kick off a backup job |
| GET | `/users/me/exports/{jobId}` | – | `200 ExportJobDTO` | Poll job status |
| GET | `/users/me/exports/{jobId}/download` | – | `200`, `application/zip` stream, `Content-Disposition: attachment` | Download once COMPLETED |
| GET | `/users/me/exports` | – | `200 List<ExportJobDTO>` | Job history (cheap to add, matches the list-existing-resource convention used elsewhere in this API) |

### Job lifecycle

1. `POST` creates a row (`PENDING`), returns immediately (`202`), launches a background coroutine.
2. Coroutine sets `RUNNING`, reads the caller's books/members/lendings/genres from existing repositories, writes CSVs, zips, sets `file_path`, `completed_on`, `status = COMPLETED`.
3. On any exception during generation: `status = FAILED`, `error` populated with a short message.
4. Retention: each job gets an `expires_on` (e.g. `created_on + 24h`). Expired jobs are treated as gone by the download endpoint (410); actual file cleanup can be a simple lazy-delete-on-expiry-check or scheduled sweep — implementation detail for the plan, not the API contract.

### Errors

| Code | Cause |
|---|---|
| 401 | Missing/invalid auth token |
| 404 | Job not found, or job belongs to a different user (never leak existence of another user's job) |
| 409 | Download requested before job reached `COMPLETED` |
| 410 | Job past `expires_on` |
| 500 | Job creation failure |

## Out of scope

- JSON export, Print Catalogue — dropped per requirement ("CSV only, rest will be removed").
- Wiring `defaultLoanDurationDays` into the actual lending-creation flow (`LendingService`) — this design only stores/returns the preference; consuming it elsewhere is a separate change.
- Scheduled cleanup mechanism for expired export files — noted as needed, left to the implementation plan.
