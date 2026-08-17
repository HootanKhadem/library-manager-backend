# Seed Default Preferences on User Creation — Design

Date: 2026-08-17
Status: Approved

## Context

The Preferences page (GET/PUT `/api/preferences`) is already fully implemented — see
[2026-08-06-user-profile-preferences-export-design.md](2026-08-06-user-profile-preferences-export-design.md).
That design deliberately used lazy-creation: no `user_preference` row exists until the
user's first `PUT`; `GET` falls back to schema defaults when no row is found.

The new requirement: when a user account is created, eagerly seed a `user_preference` row
with default values, mirroring how `CreateUserService` already seeds default genres via
`GenreRepository.seedDefaults(userId)`.

## Default values seeded

Only the schema-level defaults — no placeholder/demo text:

| Field | Seeded value |
|---|---|
| `libraryName` | `null` |
| `ownerName` | `null` |
| `description` | `null` |
| `defaultLoanDurationDays` | `30` |
| `dateFormat` | `"DD MMM YYYY"` |
| `language` | `"en"` |

Identical to what `GET /api/preferences` already returns today for a user with no row —
this change just makes the row exist earlier (at signup) instead of on first save.

## Changes

### `UserPreferenceRepository` (interface)

Add:

```kotlin
suspend fun seedDefaults(userId: Long)
```

### `PSQLUserPreferenceRepository` (impl)

Insert one `UserPreferenceDAO` row: `userId`, `createdOn`/`modifiedOn` = now (as string,
matching existing convention), `createdBy`/`modifiedBy` = userId, all other fields left
unset (column defaults apply — `defaultLoanDurationDays=30`, `dateFormat="DD MMM YYYY"`,
`language="en"`, nullable fields null). Same shape as `PSQLGenreRepository.seedDefaults`.

### `CreateUserService`

Add `userPreferenceRepository: UserPreferenceRepository` as a constructor dependency.
In `createNewUser`, call `userPreferenceRepository.seedDefaults(savedUser.id!!)` right
after the existing `genreRepository.seedDefaults(savedUser.id!!)` call.

### `DependencyInjection.kt`

Update the `CreateUserServiceInterface` binding to also pass `userPreferenceRepository = resolve()`.

## Transaction semantics (important caveat)

`withTransaction` in this codebase always opens a **new top-level transaction**
(`inTopLevelSuspendTransaction`). `genreRepository.seedDefaults` is therefore *not*
actually atomic with the user insert today — it's a sequential call after
`userRepository.save` has already committed. This change follows the exact same
pattern for consistency:

- Preference seeding runs synchronously, right after genre seeding.
- Any exception propagates up (not swallowed) — `createNewUser` fails loudly if seeding fails.
- If preference seeding fails *after* the user row has already committed, the user row is
  **not** rolled back. This is a pre-existing limitation shared with genre seeding, not a
  regression introduced here. Fixing it would require reworking the shared `withTransaction`
  helper used by every repository — out of scope for this change.

## Out of scope

- No changes to the GET/PUT `/api/preferences` endpoints, `UserPreference` DTO, validation
  rules, or the `V4__user_preference_and_export_job.sql` migration — all already correct.
- No true cross-repository transactional atomicity — noted above, out of scope.
- No change to what `GET /api/preferences` returns for existing users created before this
  change (they simply keep hitting the lazy-create fallback path, which returns identical
  values).

## Testing

- `PSQLUserPreferenceRepository` test: `seedDefaults` creates a row with expected default
  values, retrievable via `findByUserId`.
- `CreateUserService` test (new file — none currently exists for this service): creating a
  user results in a preference row with default values being seeded, alongside the existing
  default-genre seeding behavior.
