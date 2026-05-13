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
    fun `test create author with audit fields`() = testApplication {
        setupLibraryApp()
        val token = createToken()

        val author = Author(
            name = "Audit Author",
            image = "audit.jpg",
            userId = 3L,
            createdOn = "2024-01-01T00:00:00",
            createdBy = 1L,
            modifiedOn = "2024-01-01T00:00:00",
            modifiedBy = 1L
        )
        val response = client.post("/api/author") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(author))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val created = gson.fromJson(response.bodyAsText(), Author::class.java)
        assertEquals(3L, created.userId)
        assertEquals(1L, created.createdBy)

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
}
