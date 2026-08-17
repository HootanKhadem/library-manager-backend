# Refresh Token Flow Design

Date: 2026-08-17
Status: Approved

## Stack note

Repo is Kotlin/Ktor (Maven, JVM), Exposed ORM over Postgres, Flyway migrations. No Node.js involved.

## Problem

Users get logged out when the 1h access token expires, even though a 7-day refresh token is already being issued and cookied on login. There's no mechanism that actually uses the refresh token, no `/auth/refresh` (or equivalent), and no logout endpoint. Additionally, both cookies currently lack `httpOnly`.

## Goals

- Access token auto-renews transparently — no client-side code, no explicit refresh call, no 401-then-retry dance.
- Logout endpoint clears both auth cookies.
- Everything lives in cookies (no tokens in response bodies).

## Decisions

| Decision | Choice | Why |
|---|---|---|
| Refresh token storage | Stateless signed JWT, no DB | Simpler, matches existing half-implementation. Accepted tradeoff: no server-side revocation — see Caveats. |
| Refresh trigger | Transparent, inside the JWT auth plugin's `authHeader` step | Client makes zero changes; a request with an expired access token but valid refresh token just succeeds. |
| Refresh token lifetime | Fixed 7-day absolute expiry from login, never rotated/extended | Simplicity; bounds max session length regardless of activity. |
| Login response body | User info only, no raw tokens | Cookies (`httpOnly`) become the only place tokens live. |
| Logout auth requirement | Public, unconditional | Must work even when the current session is already broken/expired — the exact case it exists for. |
| Cookie attributes | `httpOnly`, `secure`, `sameSite=None`, `path=/` | Frontend is a different origin from the API, so cross-site cookies need `SameSite=None`; `httpOnly` blocks JS access (currently missing). |
| CORS | New plugin, `allowCredentials=true`, explicit env-configured origin | `SameSite=None` + credentialed cross-origin requests require CORS with a concrete origin — wildcard doesn't work with credentials. |

## Security fix folded into this work

Today `access_token` and `refresh_token` are structurally identical JWTs — same claims (`email`, `role`, `userId`), same secret, differing only in `exp`. A leaked refresh token can already be used directly as a full access token. This design adds a `type: "access" | "refresh"` claim and enforces it at verification time, closing that hole.

## Architecture

### Token claims (`JwtService.kt`)

`signToken` gains a `type` claim. `generateToken(user)` signs an access token (`type=access`, 1h) and a refresh token (`type=refresh`, 7d), same as today otherwise.

New methods:
- `verify(token: String): DecodedJWT?` — runs the existing `JWT.require(...)` verifier (signature, issuer, audience, expiry), returns `null` on any `JWTVerificationException` instead of throwing.
- `refreshAccessToken(refreshToken: String): String?` — `verify()`s the token, confirms `type == "refresh"`, and if valid re-signs a fresh access token from its `email`/`role`/`userId` claims. Returns `null` on any failure.

### Transparent refresh (`JWT.kt`)

`configureJWT()` resolves `JwtService` from DI (already provided in `DependencyInjection.kt`). The `authHeader` block changes from "read `access_token` cookie, else parse `Authorization` header" to:

1. Read `access_token` cookie. If present and `jwtService.verify(it)` succeeds with `type=access` → use it as the Bearer credential, proceed as today.
2. Else read `refresh_token` cookie. If present, call `jwtService.refreshAccessToken(it)`:
   - Success → `call.response.cookies.append("access_token", newToken, httpOnly=true, secure=true, sameSite=None, path="/", expires=now+1h)`, then use `newToken` as the Bearer credential for this request. The `verifier`/`validate` steps run normally against it, so the request completes as authenticated with no 401 ever surfaced.
   - Failure (missing/expired/wrong-type/tampered refresh token) → fall through.
3. Else → fall back to `parseAuthorizationHeader()` (existing behavior), which yields the normal 401 challenge when nothing validates.

This is the only place the "auto-refresh" logic lives — no new route, no interceptor needed on the client.

### Login / Logout (`AuthenticationRoutes.kt`)

- `POST /auth/login`: unchanged credential check. `appendTokensToCookies` sets both cookies with `httpOnly=true, secure=true, sameSite=None, path="/"` (adds `httpOnly`, which is currently missing). Response body drops `access_token`/`refresh_token` and returns user info only (e.g. `{"email": ..., "role": ...}`).
- `POST /auth/logout` (new, in `configurePublicRouting()`, no `authenticate` wrapper): clears both cookies by setting them with an epoch expiry / `maxAge=0`, same attributes otherwise. Always responds 200 — cannot itself fail due to auth state.

### CORS (`HTTP.kt`, `application.yaml`, `pom.xml`)

- New dependency `ktor-server-cors-jvm` (matching version of other `ktor-server-*` deps in `pom.xml`).
- `application.yaml`: add `ktor.cors.allowedOrigin: ${CORS_ALLOWED_ORIGIN:http://localhost:3000}`.
- `HTTP.kt` (`configureHTTP()`): `install(CORS) { allowCredentials = true; allowHost(configuredOrigin, schemes = listOf("http", "https")); allowHeader(HttpHeaders.ContentType); allowMethod(Get/Post/Put/Delete/Patch) }`.

## Caveats (accepted tradeoffs)

- **No server-side revocation.** Because refresh tokens are stateless JWTs, logout can only clear the browser's cookies. A refresh token captured before logout (e.g. via XSS despite `httpOnly`, or device theft) remains valid until its own 7-day expiry. Fixing this would require persisting refresh tokens (DB-backed, revocable) — explicitly declined in favor of simplicity.
- **Fixed 7-day absolute session.** An active user is still forced to log in again after 7 days; sessions are not extended by activity.
- **Role/userId staleness on transparent refresh.** `JwtService.refreshAccessToken()` mints the new access token by copying the `role` and `userId` claims straight out of the refresh token, with no database re-check. This is a real behavior change from before this feature existed: previously, a role change or account deletion took effect the next time the (≤1h-lived) access token expired and the user had to re-authenticate. Now that access tokens are refreshed transparently, a role change or account deletion doesn't take effect until the user's refresh token itself expires — i.e. up to 7 days — because the refresh path never touches the DB.
- **CSRF posture under `SameSite=None`.** Cookies (including `refresh_token`) are set with `SameSite=None` so they're sent cross-site, which is what makes transparent refresh work from the frontend's origin. This is safe against classic CSRF because every mutating route in this app reads its body with `call.receive<DTO>()` under JSON content negotiation — there is no `receiveParameters`/`receiveMultipart` handler anywhere in `src/main` — so a cross-site `<form>` POST (which can only submit `application/x-www-form-urlencoded` or `multipart/form-data`, not arbitrary JSON) cannot produce a body the server will parse, and CORS restricts which origins ever get to read a JSON response even on requests that do go through (e.g. via `fetch`). The one endpoint reachable by a bodyless cross-site request is `POST /auth/logout`, which takes no body at all — a malicious page can force a visitor's session to log out, which is a nuisance, not a data or session breach. Any future endpoint added with a form-encoded (`receiveParameters`) or multipart body would break this assumption and needs its own CSRF protection (e.g. a double-submit token) before relying on `SameSite=None` alone.

## Testing plan

**Unit** (`JwtService`):
- `verify`: valid token → `DecodedJWT`; expired, wrong secret, wrong issuer/audience, malformed → `null`.
- `refreshAccessToken`: valid refresh token → new access token with `type=access` and same `email`/`role`/`userId`; access token passed in (wrong type) → `null`; expired/tampered refresh token → `null`.

**Integration** (Ktor `testApplication`):
- Login: response body has no `access_token`/`refresh_token` keys; both `Set-Cookie` headers carry `HttpOnly`, `Secure`, `SameSite=None`.
- Valid access cookie → protected route succeeds, no new `Set-Cookie`.
- Expired access cookie + valid refresh cookie → protected route succeeds AND response carries a new `Set-Cookie: access_token=...`.
- Expired access cookie + expired/missing refresh cookie → 401.
- Logout with no cookies, with valid cookies, and with expired cookies → all return 200 and clear both cookies.
- CORS: preflight from configured origin succeeds with credentials; a different origin is rejected.
