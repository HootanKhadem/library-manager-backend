package routes.book

import com.dw.model.dto.Author
import com.dw.model.dto.Book
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
}
