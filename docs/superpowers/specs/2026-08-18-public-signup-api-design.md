# Public Signup API Design

Date: 2026-08-18
Status: Approved

## Stack note

Repo is Kotlin/Ktor (Maven, JVM), Exposed ORM over Postgres (H2 in tests), Flyway migrations, DI via Ktor's `dependencies` plugin.

## Problem

The only way to create a user today is `POST /admin/users`, gated behind `Role.ADMIN` (`RoleAuthorizationPlugin`, `withRole`). There is no self-service signup — a new user can only be created by an existing admin. The request also wants preferences (added recently via `UserPreference`/`UserPreferenceRepository`/`UserPreferenceService`) to be settable at account-creation time, not just via the separate `/api/preferences` route after the fact.

## Goals

- Anyone can create their own account via a public, unauthenticated endpoint.
- Signup accepts the same fields the admin flow did (`name`, `email`, `password`) plus optional preferences.
- A self-registered account can never be granted `ADMIN` — `role` is not client-controlled.
- Successful signup logs the user in immediately (same cookie mechanism as `/auth/login`), avoiding an extra round trip.
- Duplicate email is a clean `409`, not a raw DB constraint violation surfacing as `500`.

## Non-goals

- Email verification / confirmation flow.
- Rate limiting or CAPTCHA on signup (out of scope; can be added later at the HTTP/infra layer).
- Any admin-only user-creation path — this design removes `/admin/users` outright, not adds alongside it (per explicit decision below).

## Decisions

| Decision | Choice | Why |
|---|---|---|
| Endpoint scope | Fully replace `/admin/users`; no admin-only create-user endpoint remains | User explicitly asked to "remove it from the admin gated jwt role." No other code path creates non-bootstrap admins via the API today, so nothing is lost. |
| Role handling | `SignupRequest` has no `role` field at all; service hardcodes `Role.USER` | Public endpoint — trusting a client-supplied role would let anyone self-promote to `ADMIN`. |
| Preferences input | Optional `preferences: UserPreference?` on `SignupRequest`. If present, validated and saved via existing `UserPreferenceServiceInterface.savePreferences`; if absent, `UserPreferenceRepository.seedDefaults` runs (today's behavior, unchanged) | Reuses existing validation (`language`, `dateFormat`, `defaultLoanDurationDays`) instead of duplicating it. |
| Post-signup session | Auto-login: generate access/refresh JWTs and set them as cookies exactly like `/auth/login`, respond `201` with `{name, email, role}` | Matches login's response shape; new user doesn't need a second request. |
| Duplicate email | Check `UserRepository.findByEmail` before insert; throw `EmailAlreadyExistsException` → `409 Conflict` | Avoids relying on catching a DB-level unique-constraint exception; keeps the check explicit and testable. |
| Password rule | Min 8 chars, ≥1 uppercase letter, ≥1 digit; violation → `400` with a message | Public internet-facing endpoint needs a basic strength floor. No such check existed for the admin flow; this is new. |
| Route path | `POST /auth/signup`, alongside `/auth/login` and `/auth/logout` in `configurePublicRouting()` | Groups all unauthenticated auth-lifecycle endpoints under one prefix. |

## Architecture

### `SignupRequest` DTO (`model/dto/UserDTO.kt`)

```kotlin
@Serializable
data class SignupRequest(
    val name: String,
    val email: String,
    val password: String,
    val preferences: UserPreference? = null
)
```

No `id`, `role`, `salt`, `createdBy`, etc. — those are either server-assigned or forbidden for a public caller.

### `EmailAlreadyExistsException` (new file, `EmailAlreadyExistsException.kt`, mirrors `UserNotFoundException.kt`)

```kotlin
class EmailAlreadyExistsException(message: String) : RuntimeException(message)
```

### `SignupService` (replaces `service/admin/CreateUserService.kt` → `service/authentication/SignupService.kt`)

```kotlin
interface SignupServiceInterface {
    suspend fun signup(request: SignupRequest): Pair<UserDTO, Pair<String, String>> // user, (accessToken, refreshToken)
}
```

`signup()`:
1. `userRepository.findByEmail(request.email) != null` → throw `EmailAlreadyExistsException("Email already registered: ${request.email}")`.
2. Validate password: `require(password.length >= 8 && password.any { it.isUpperCase() } && password.any { it.isDigit() })` — a single `require` with a combined message, or three separate `require`s each with a specific message (implementation picks whichever reads cleaner; three separate is preferred so the error pinpoints which rule failed).
3. Hash password (`PasswordUtil`, same as before), build `UserDTO(name, email, password = hashed, role = Role.USER, salt, createdOn = now, createdBy = null, modifiedOn = now, modifiedBy = null)`.
4. `userRepository.save(...)`.
5. `genreRepository.seedDefaults(savedUser.id!!)` (unchanged from today's admin flow).
6. Preferences: if `request.preferences != null` → `userPreferenceService.savePreferences(savedUser.id, request.preferences)` (this validates language/dateFormat/loanDuration and throws `IllegalArgumentException` on bad input, which propagates up); else → `userPreferenceRepository.seedDefaults(savedUser.id!!)`.
7. `jwtService.generateToken(savedUser)` → tokens.
8. Return `savedUser to tokens`.

Constructor deps: `userRepository`, `genreRepository`, `userPreferenceRepository`, `userPreferenceService`, `jwtService`.

### Route (`routes/user/AuthenticationRoutes.kt`)

```kotlin
fun Route.signup(signupService: SignupServiceInterface) {
    post("/auth/signup") {
        val request = call.receive<SignupRequest>()
        try {
            val (user, tokens) = signupService.signup(request)
            appendTokensToCookies(tokens)
            call.respond(HttpStatusCode.Created, mapOf("name" to user.name, "email" to user.email, "role" to user.role.name))
        } catch (e: EmailAlreadyExistsException) {
            call.respond(HttpStatusCode.Conflict, mapOf("error" to e.message))
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
        }
    }
}
```

Reuses the existing private `appendTokensToCookies` helper already in this file (used by `login`).

### Wiring

- `Routing.kt`: `configurePublicRouting()` resolves `SignupServiceInterface` and calls `signup(signupService)` alongside `login`/`logout`. Remove `configureAdminRouting()` entirely (function, its call in `Application.kt`, its call in `BaseRouteTest.kt`), and remove the `adminCreateUser` import/route.
- Delete `routes/admin/AdminUserRoutes.kt`, `service/admin/CreateUserService.kt`.
- `DependencyInjection.kt`: replace the `CreateUserServiceInterface` provide with:
  ```kotlin
  provide<SignupServiceInterface> {
      SignupService(
          userRepository = resolve(),
          genreRepository = resolve(),
          userPreferenceRepository = resolve(),
          userPreferenceService = resolve(),
          jwtService = jwtService
      )
  }
  ```
- `Role.ADMIN` enum value stays (still used by the JWT `role` claim and by `UserRepository.createAdminUser()`, the startup bootstrap that seeds the one admin account from config — untouched by this change). No route grants `ADMIN` anymore; it only ever exists via that bootstrap.

## Error handling summary

| Condition | Response |
|---|---|
| Email already registered | `409 Conflict`, `{"error": "..."}` |
| Password fails strength rule | `400 Bad Request`, `{"error": "..."}` |
| Preferences fail existing validation (bad language/dateFormat/loanDuration) | `400 Bad Request`, `{"error": "..."}` |
| Success | `201 Created`, `{"name", "email", "role"}` + `Set-Cookie` for access + refresh tokens (same attributes as login: `httpOnly`, `secure`, `sameSite=None`, `path=/`) |

## Testing plan

**Unit** (`SignupServiceTest`, replaces `CreateUserServiceTest`):
- Happy path: new email → user saved with `role = USER`, `createdBy = null`; genre defaults seeded; preference defaults seeded when `preferences` omitted; tokens returned.
- Preferences provided → `UserPreferenceService.savePreferences` called with the given values instead of `seedDefaults`.
- Duplicate email → `EmailAlreadyExistsException`, `userRepository.save` never called.
- Password missing uppercase / missing digit / under 8 chars → `IllegalArgumentException`, each case.
- Invalid preferences (e.g. bad `language`) → propagates `IllegalArgumentException` from `UserPreferenceService`.

**Integration** (`AuthenticationRoutesTest`, extends existing login/logout tests):
- `POST /auth/signup` with valid body, no auth header → `201`, `Set-Cookie` present for both tokens, body has `name`/`email`/`role=USER`.
- Duplicate email → `409`.
- Weak password (too short / no uppercase / no digit) → `400`.
- Signup with `preferences` block → subsequent `GET /api/preferences` (authenticated with the returned cookie) reflects the submitted values.
- Signup without `preferences` → `GET /api/preferences` reflects defaults.
- `POST /admin/users` no longer exists → `404` (route removed). Existing `AdminUserRoutesTest.kt` is deleted since the endpoint it tests no longer exists.

## Caveats (accepted tradeoffs)

- **No email verification.** Anyone can register with any email string in valid-looking form (no format validation beyond what the DB/serialization already does); no confirmation email is sent. Consistent with "no non-goals beyond what's asked."
- **No rate limiting.** A public signup endpoint is a target for automated account creation; this is explicitly deferred as infra-level concern (e.g. reverse proxy / WAF), not application code.
