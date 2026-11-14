package routes.author

import com.dw.model.dto.Author
import com.dw.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import routes.BaseRouteTest
import kotlin.test.*

class AuthorRoutesTest : BaseRouteTest() {

    override val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:author_route_test;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver",
        "ktor.jwt.secret" to "secret",
        "ktor.jwt.issuer" to "issuer",
        "ktor.jwt.audience" to "audience",
        "ktor.jwt.realm" to "realm"
    )

    @Test
    fun `test create author`() = testApplication {
        setupLibraryApp()
        val token = createToken()

        val author = Author(name = "New Author", image = "new.jpg")
        val response = client.post("/api/author") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(author))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val createdAuthor = gson.fromJson(response.bodyAsText(), Author::class.java)
        assertEquals(author.name, createdAuthor.name)
        assertNotNull(createdAuthor.id)

        cleanup()
    }

    @Test
    fun `test create author with audit fields from the authenticated user`() = testApplication {
        setupLibraryApp()
        val token = createToken(userId = 3L)

        val author = Author(
            name = "Audit Author",
            image = "audit.jpg",
            createdOn = "2024-01-01T00:00:00",
            modifiedOn = "2024-01-01T00:00:00"
        )
        val response = client.post("/api/author") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(author))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val created = gson.fromJson(response.bodyAsText(), Author::class.java)
        assertEquals(3L, created.userId)

        cleanup()
    }

    @Test
    fun `test create author returns 401 when unauthenticated`() = testApplication {
        setupLibraryApp()

        val response = client.post("/api/author") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(Author(name = "No Auth Author", image = "noauth.jpg")))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)

        cleanup()
    }

    @Test
    fun `test search authors`() = testApplication {
        setupLibraryApp()
        val token = createToken()

        client.post("/api/author") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(Author(name = "Author One", image = "1.jpg")))
        }
        client.post("/api/author") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(Author(name = "Author Two", image = "2.jpg")))
        }

        val response = client.get("/api/author/search?query=One") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, response.status)

        val results = gson.fromJson(response.bodyAsText(), Array<Author>::class.java)
        assertEquals(1, results.size)
        assertEquals("Author One", results[0].name)

        cleanup()
    }

    // ── POST /api/author/merge ───────────────────────────────────────────────

    @Test
    fun `POST api author merge merges, 400 on bad request, 404 for another user, 401 unauthenticated`() = testApplication {
        setupLibraryApp()
        val owner = createToken(userId = 91L)
        val other = createToken(userId = 92L)

        suspend fun create(name: String, token: String) = gson.fromJson(
            client.post("/api/author") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(gson.toJson(Author(name = name, image = "")))
            }.bodyAsText(), Author::class.java
        ).id!!
        suspend fun merge(token: String?, body: String) = client.post("/api/author/merge") {
            token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(body)
        }

        val target = create("Target", owner)
        val source = create("Source", owner)
        val foreign = create("Foreign", other)

        assertEquals(HttpStatusCode.Unauthorized, merge(null, """{"targetId":$target,"sourceIds":[$source]}""").status)
        assertEquals(HttpStatusCode.BadRequest, merge(owner, """{"targetId":$target,"sourceIds":[]}""").status)
        assertEquals(HttpStatusCode.BadRequest, merge(owner, """{"targetId":$target,"sourceIds":[$target]}""").status)
        assertEquals(HttpStatusCode.NotFound, merge(owner, """{"targetId":$target,"sourceIds":[$foreign]}""").status)

        val ok = merge(owner, """{"targetId":$target,"sourceIds":[$source],"name":"Merged"}""")
        assertEquals(HttpStatusCode.OK, ok.status)
        assertEquals("Merged", gson.fromJson(ok.bodyAsText(), Author::class.java).name)
        val gone = client.get("/api/author/$source") { header(HttpHeaders.Authorization, "Bearer $owner") }
        assertEquals(HttpStatusCode.NotFound, gone.status)

        cleanup()
    }

    // ── GET /api/author (paginated) ──────────────────────────────────────────

    @Test
    fun `GET api author returns first page of authors for user`() = testApplication {
        setupLibraryApp()
        val token = createToken(userId = 70L)

        client.post("/api/author") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(Author(name = "List Author 1", image = "1.jpg", userId = 70L)))
        }
        client.post("/api/author") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(Author(name = "List Author 2", image = "2.jpg", userId = 70L)))
        }

        val response = client.get("/api/author?page=1&pageSize=1") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val json = gson.fromJson(response.bodyAsText(), com.google.gson.JsonObject::class.java)
        assertEquals(1, json.getAsJsonArray("items").size())
        assertEquals(2, json.get("totalItems").asInt)
        assertEquals(2, json.get("totalPages").asInt)

        cleanup()
    }

    @Test
    fun `GET api author returns 401 when unauthenticated`() = testApplication {
        setupLibraryApp()

        val response = client.get("/api/author")
        assertEquals(HttpStatusCode.Unauthorized, response.status)

        cleanup()
    }

    @Test
    fun `GET api author by id returns own author with book count and 404 for another user`() = testApplication {
        setupLibraryApp()
        val owner = createToken(userId = 81L)
        val other = createToken(userId = 82L)

        val created = client.post("/api/author") {
            header(HttpHeaders.Authorization, "Bearer $owner")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(Author(name = "Solo Author", image = "")))
        }
        val id = gson.fromJson(created.bodyAsText(), Author::class.java).id

        val ok = client.get("/api/author/$id") { header(HttpHeaders.Authorization, "Bearer $owner") }
        assertEquals(HttpStatusCode.OK, ok.status)
        val body = gson.fromJson(ok.bodyAsText(), Author::class.java)
        assertEquals("Solo Author", body.name)
        assertEquals(0L, body.bookCount)

        val notMine = client.get("/api/author/$id") { header(HttpHeaders.Authorization, "Bearer $other") }
        assertEquals(HttpStatusCode.NotFound, notMine.status)

        cleanup()
    }
}
