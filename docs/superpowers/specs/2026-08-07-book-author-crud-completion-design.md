# Book/Author CRUD Completion — Design

Date: 2026-08-07

## Problem

Frontend Books-page table and Authors-page grid are stuck on mock data because the
backend is missing:

- `list-all-books` (paginated)
- `list-all-authors` (paginated)
- `book update` (route only — service/repo already support it)
- `book delete` (route only — service/repo already support it)

Author update/delete are explicitly out of scope (not requested; no frontend UI
consumes them yet).

## Existing conventions this design follows

- Route layer reads `userId` from `JWTPrincipal` claim (`routes/genre/GenreRoutes.kt`,
  `routes/lending/LendingRoutes.kt`, `routes/aggregation/DashboardRoutes.kt`); 401 if
  missing.
- All list endpoints are per-user scoped (`findAllByUserId`, `getGenresByUserId`).
- Repository → Service → Route three-layer split, one interface + one `PSQL*`
  implementation per repository.
- `withTransaction {}` wraps all Exposed DB access in `PSQL*Repository` classes.

## 1. Pagination wrapper

New file `model/dto/PagedResponse.kt`:

```kotlin
@Serializable
data class PagedResponse<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalItems: Long,
    val totalPages: Int
)
```

- `page` is 1-based.
- Query params: `page` (default 1), `pageSize` (default 20).
- Clamping, not rejection: `page < 1` → 1. `pageSize < 1` → 1. `pageSize > 100` → 100.
- Requesting a page past the end returns `200 OK` with `items = []`, not 404.
- `totalPages = ceil(totalItems / pageSize)`, minimum 1 (even when `totalItems == 0`,
  matches typical paging UX — an empty first page still "exists").

## 2. Repository layer

`db/BookRepository.kt` — add:

```kotlin
suspend fun findAllByUserIdPaged(userId: Long, page: Int, pageSize: Int): List<Book>
```

(`countByUserId` already exists — reused for `totalItems`.)

`db/AuthorRepository.kt` — add:

```kotlin
suspend fun findAllByUserIdPaged(userId: Long, page: Int, pageSize: Int): List<Author>
suspend fun countByUserId(userId: Long): Long
```

Both `PSQL*Repository` implementations use Exposed's `.limit(pageSize).offset((page - 1).toLong() * pageSize)`
on the existing `findAllByUserId`-style query, ordered by `id` ascending for stable
paging (no ordering guarantee currently exists on `findAllByUserId`, so this is a new
but uncontroversial addition).

## 3. Service layer

`service/book/BookServiceInterface.kt`:

```kotlin
suspend fun getAllBooksPaged(userId: Long, page: Int, pageSize: Int): PagedResponse<Book>

// signatures change — requesterId added for ownership enforcement:
suspend fun updateBook(id: Long, book: Book, requesterId: Long): Book?
suspend fun deleteBook(id: Long, requesterId: Long): Boolean
```

`updateBook`/`deleteBook` impl: fetch the existing book by id first. If it doesn't
exist, or `existing.userId != requesterId`, return `null`/`false` — the route layer
turns both cases into a single `404 Not Found` (deliberately not `403`, to avoid
leaking whether an id owned by another user exists).

`service/author/AuthorServiceInterface.kt`:

```kotlin
suspend fun getAllAuthorsPaged(userId: Long, page: Int, pageSize: Int): PagedResponse<Author>
```

## 4. Routes

`routes/book/BookRoutes.kt` — add, each requiring JWT `userId` (401 if absent):

```kotlin
get("/api/book") {
    // reads page/pageSize query params, clamps, calls getAllBooksPaged
}

put("/api/book/{id}") {
    // 400 if id path param isn't a valid Long
    // 404 if updateBook(id, body, userId) returns null
}

delete("/api/book/{id}") {
    // 400 if id path param isn't a valid Long
    // 204 if deleteBook(id, userId) returns true, else 404
}
```

`routes/author/AuthorRoutes.kt` — add:

```kotlin
get("/api/author") {
    // reads page/pageSize query params, clamps, calls getAllAuthorsPaged
}
```

## 5. Error handling summary

| Case | Response |
|---|---|
| Missing/invalid JWT `userId` claim | 401 |
| `PUT`/`DELETE /api/book/{id}` — malformed id | 400 |
| `PUT`/`DELETE /api/book/{id}` — not found OR owned by another user | 404 |
| `page`/`pageSize` out of range | clamped, 200 (never rejected) |
| Page beyond last page | 200, `items: []` |

## 6. Testing (TDD, tests written first)

- `BookRoutesTest` — add cases: paginated list (page 1/2, empty page), update happy
  path, update 404 (missing + wrong owner), delete happy path, delete 404 (missing +
  wrong owner), 401 unauthenticated on all three.
- `BookServiceTest` — pagination math delegated correctly to repo; ownership check
  branches (owner match / mismatch / not found).
- `PSQLBookRepositoryTest` — `findAllByUserIdPaged` limit/offset correctness across
  page boundaries.
- `AuthorRoutesTest` — paginated list cases, 401 unauthenticated.
- `AuthorServiceInterfaceImplTest` — pagination math delegated correctly.
- `PSQLAuthorRepositoryTest` (new file — doesn't exist yet) — `findAllByUserIdPaged`
  and `countByUserId` correctness.

## Out of scope

- Author update/delete (not requested).
- Genre/Member/Admin-user management UI pages (scoped out separately, proxy routes
  already exist).
- Lent-page "Remind" button backend support (not requested here).
- Retrofitting ownership checks onto existing `GET /api/book/{id}` or genre
  update/delete (pre-existing gap, out of scope for this change).
