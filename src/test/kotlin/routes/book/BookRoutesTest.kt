package routes.book

import com.dw.model.dto.Author
import com.dw.model.dto.Book
import com.dw.model.dto.Lending
import com.dw.model.dto.Member
import com.dw.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import routes.BaseRouteTest
import kotlin.test.*

class BookRoutesTest : BaseRouteTest() {

    override val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:book_route_test;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver",
        "ktor.jwt.secret" to "secret",
        "ktor.jwt.issuer" to "issuer",
        "ktor.jwt.audience" to "audience",
        "ktor.jwt.realm" to "realm"
    )

    private fun bookPayload(
        authorName: String = "Route Test Author",
        authorId: Long? = null,
        isbn: String = "isbn-route-001",
        userId: Long? = null
    ) = Book(
        name = "Route Test Book",
        author = Author(id = authorId, name = authorName, image = "author.jpg"),
        translator = "Route Translator",
        pages = 200,
        isbn = isbn,
        publishedDate = "2021-05-01",
        publisher = "Route Publisher",
        quantity = 3,
        image = "book.jpg",
        userId = userId
    )

    // ── POST /api/book ────────────────────────────────────────────────────────

    @Test
    fun `POST api book creates book with new author`() = testApplication {
        setupLibraryApp()
        val token = createToken()

        val payload = bookPayload(isbn = "isbn-new-author-route")
        val response = client.post("/api/book") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(payload))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val created = gson.fromJson(response.bodyAsText(), Book::class.java)
        assertNotNull(created.id)
        assertEquals("Route Test Book", created.name)
        assertNotNull(created.author.id)
        assertEquals("Route Test Author", created.author.name)
        assertEquals("Route Translator", created.translator)
        assertNotNull(created.createdOn)
        assertNotNull(created.modifiedOn)

        cleanup()
    }

    @Test
    fun `POST api book reuses existing author by name`() = testApplication {
        setupLibraryApp()
        val token = createToken()

        // create author first via author route
        val authorResponse = client.post("/api/author") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(Author(name = "Shared Author", image = "shared.jpg")))
        }
        val savedAuthor = gson.fromJson(authorResponse.bodyAsText(), Author::class.java)

        // create book referencing same author name (no id)
        val payload = bookPayload(authorName = "Shared Author", isbn = "isbn-shared-author")
        val response = client.post("/api/book") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(payload))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val created = gson.fromJson(response.bodyAsText(), Book::class.java)
        assertEquals(savedAuthor.id, created.author.id)

        cleanup()
    }

    @Test
    fun `POST api book sets audit fields`() = testApplication {
        setupLibraryApp()
        val token = createToken()

        val payload = bookPayload(isbn = "isbn-audit-route", userId = 10L)
        val response = client.post("/api/book") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(payload))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val created = gson.fromJson(response.bodyAsText(), Book::class.java)
        assertEquals(10L, created.userId)
        assertEquals(10L, created.createdBy)
        assertEquals(10L, created.modifiedBy)
        assertNotNull(created.createdOn)

        cleanup()
    }

    // ── GET /api/book/{id} ────────────────────────────────────────────────────

    @Test
    fun `GET api book id returns book when exists`() = testApplication {
        setupLibraryApp()
        val token = createToken()

        val createResponse = client.post("/api/book") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(bookPayload(isbn = "isbn-get-route")))
        }
        val created = gson.fromJson(createResponse.bodyAsText(), Book::class.java)

        val response = client.get("/api/book/${created.id}") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val found = gson.fromJson(response.bodyAsText(), Book::class.java)
        assertEquals(created.id, found.id)
        assertEquals("Route Test Book", found.name)

        cleanup()
    }

    @Test
    fun `GET api book id returns 404 when not exists`() = testApplication {
        setupLibraryApp()
        val token = createToken()

        val response = client.get("/api/book/99999") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)

        cleanup()
    }

    @Test
    fun `GET api book id returns 400 for invalid id`() = testApplication {
        setupLibraryApp()
        val token = createToken()

        val response = client.get("/api/book/not-a-number") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)

        cleanup()
    }

    // ── GET /api/book (paginated) ────────────────────────────────────────────

    @Test
    fun `GET api book returns first page of books for user`() = testApplication {
        setupLibraryApp()
        val token = createToken(userId = 20L)

        client.post("/api/book") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(bookPayload(isbn = "isbn-list-1", userId = 20L)))
        }
        client.post("/api/book") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(bookPayload(isbn = "isbn-list-2", userId = 20L)))
        }

        val response = client.get("/api/book?page=1&pageSize=1") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val json = gson.fromJson(response.bodyAsText(), com.google.gson.JsonObject::class.java)
        assertEquals(1, json.getAsJsonArray("items").size())
        assertEquals(1, json.get("page").asInt)
        assertEquals(1, json.get("pageSize").asInt)
        assertEquals(2, json.get("totalItems").asInt)
        assertEquals(2, json.get("totalPages").asInt)

        cleanup()
    }

    @Test
    fun `GET api book defaults to page 1 and pageSize 20 when params omitted`() = testApplication {
        setupLibraryApp()
        val token = createToken(userId = 21L)

        val response = client.get("/api/book") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val json = gson.fromJson(response.bodyAsText(), com.google.gson.JsonObject::class.java)
        assertEquals(1, json.get("page").asInt)
        assertEquals(20, json.get("pageSize").asInt)

        cleanup()
    }

    @Test
    fun `GET api book clamps pageSize above 100`() = testApplication {
        setupLibraryApp()
        val token = createToken(userId = 22L)

        val response = client.get("/api/book?pageSize=500") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val json = gson.fromJson(response.bodyAsText(), com.google.gson.JsonObject::class.java)
        assertEquals(100, json.get("pageSize").asInt)

        cleanup()
    }

    @Test
    fun `GET api book returns 401 when unauthenticated`() = testApplication {
        setupLibraryApp()

        val response = client.get("/api/book")
        assertEquals(HttpStatusCode.Unauthorized, response.status)

        cleanup()
    }

    // ── PUT /api/book id ──────────────────────────────────────────────────────

    @Test
    fun `PUT api book id updates book when caller owns it`() = testApplication {
        setupLibraryApp()
        val token = createToken(userId = 30L)

        val createResponse = client.post("/api/book") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(bookPayload(isbn = "isbn-put-owner", userId = 30L)))
        }
        val created = gson.fromJson(createResponse.bodyAsText(), Book::class.java)

        val response = client.put("/api/book/${created.id}") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(created.copy(name = "Updated Title")))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val updated = gson.fromJson(response.bodyAsText(), Book::class.java)
        assertEquals("Updated Title", updated.name)

        cleanup()
    }

    @Test
    fun `PUT api book id returns 404 when caller does not own book`() = testApplication {
        setupLibraryApp()
        val ownerToken = createToken(userId = 40L)
        val otherToken = createToken(userId = 41L)

        val createResponse = client.post("/api/book") {
            header(HttpHeaders.Authorization, "Bearer $ownerToken")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(bookPayload(isbn = "isbn-put-not-owner", userId = 40L)))
        }
        val created = gson.fromJson(createResponse.bodyAsText(), Book::class.java)

        val response = client.put("/api/book/${created.id}") {
            header(HttpHeaders.Authorization, "Bearer $otherToken")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(created.copy(name = "Hacked Title")))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)

        cleanup()
    }

    @Test
    fun `PUT api book id returns 400 for invalid id`() = testApplication {
        setupLibraryApp()
        val token = createToken()

        val response = client.put("/api/book/not-a-number") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(bookPayload()))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)

        cleanup()
    }

    @Test
    fun `PUT api book id returns 401 when unauthenticated`() = testApplication {
        setupLibraryApp()

        val response = client.put("/api/book/1") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(bookPayload()))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)

        cleanup()
    }

    // ── DELETE /api/book id ───────────────────────────────────────────────────

    @Test
    fun `DELETE api book id removes book when caller owns it`() = testApplication {
        setupLibraryApp()
        val token = createToken(userId = 50L)

        val createResponse = client.post("/api/book") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(bookPayload(isbn = "isbn-delete-owner", userId = 50L)))
        }
        val created = gson.fromJson(createResponse.bodyAsText(), Book::class.java)

        val response = client.delete("/api/book/${created.id}") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.NoContent, response.status)

        val getResponse = client.get("/api/book/${created.id}") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.NotFound, getResponse.status)

        cleanup()
    }

    @Test
    fun `DELETE api book id returns 404 when caller does not own book`() = testApplication {
        setupLibraryApp()
        val ownerToken = createToken(userId = 60L)
        val otherToken = createToken(userId = 61L)

        val createResponse = client.post("/api/book") {
            header(HttpHeaders.Authorization, "Bearer $ownerToken")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(bookPayload(isbn = "isbn-delete-not-owner", userId = 60L)))
        }
        val created = gson.fromJson(createResponse.bodyAsText(), Book::class.java)

        val response = client.delete("/api/book/${created.id}") {
            header(HttpHeaders.Authorization, "Bearer $otherToken")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)

        cleanup()
    }

    @Test
    fun `DELETE api book id returns 409 when book has lending history`() = testApplication {
        setupLibraryApp()
        val token = createToken(userId = 70L)

        val createResponse = client.post("/api/book") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(bookPayload(isbn = "isbn-delete-lent-route", userId = 70L)))
        }
        val created = gson.fromJson(createResponse.bodyAsText(), Book::class.java)

        val memberResponse = client.post("/api/member") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(Member(name = "Borrower", email = "borrower-route@example.com", password = "pass")))
        }
        val member = gson.fromJson(memberResponse.bodyAsText(), Member::class.java)

        client.post("/api/lending") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(Lending(bookId = created.id!!, memberId = member.id, lentDate = "2026-06-16")))
        }

        val response = client.delete("/api/book/${created.id}") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.Conflict, response.status)

        // book must still exist
        val getResponse = client.get("/api/book/${created.id}") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, getResponse.status)

        cleanup()
    }

    @Test
    fun `DELETE api book id returns 400 for invalid id`() = testApplication {
        setupLibraryApp()
        val token = createToken()

        val response = client.delete("/api/book/not-a-number") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)

        cleanup()
    }

    @Test
    fun `DELETE api book id returns 401 when unauthenticated`() = testApplication {
        setupLibraryApp()

        val response = client.delete("/api/book/1")
        assertEquals(HttpStatusCode.Unauthorized, response.status)

        cleanup()
    }
}
