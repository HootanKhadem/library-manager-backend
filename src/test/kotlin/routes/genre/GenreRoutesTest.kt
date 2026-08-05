package routes.genre

import com.dw.model.dto.Genre
import com.dw.model.dto.Role
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import routes.BaseRouteTest
import kotlin.test.*

class GenreRoutesTest : BaseRouteTest() {

    override val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:genre_route_test;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver",
        "ktor.jwt.secret" to "secret",
        "ktor.jwt.issuer" to "issuer",
        "ktor.jwt.audience" to "audience",
        "ktor.jwt.realm" to "realm"
    )

    @Test
    fun `POST api genre creates genre and returns 201`() = testApplication {
        setupLibraryApp()
        val token = createToken(userId = 1L, role = Role.USER)

        val response = client.post("/api/genre") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(Genre(name = "Fantasy", userId = 1L)))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val created = gson.fromJson(response.bodyAsText(), Genre::class.java)
        assertNotNull(created.id)
        assertEquals("Fantasy", created.name)

        cleanup()
    }

    @Test
    fun `GET api genre returns genres for authenticated user`() = testApplication {
        setupLibraryApp()
        val token = createToken(userId = 1L, role = Role.USER)

        client.post("/api/genre") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(Genre(name = "Thriller", userId = 1L)))
        }

        val response = client.get("/api/genre") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val genres = gson.fromJson(response.bodyAsText(), Array<Genre>::class.java)
        assertTrue(genres.any { it.name == "Thriller" })

        cleanup()
    }

    @Test
    fun `PUT api genre id updates genre`() = testApplication {
        setupLibraryApp()
        val token = createToken(userId = 1L, role = Role.USER)

        val createResponse = client.post("/api/genre") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(Genre(name = "OldName", userId = 1L)))
        }
        val created = gson.fromJson(createResponse.bodyAsText(), Genre::class.java)

        val response = client.put("/api/genre/${created.id}") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(Genre(name = "NewName", userId = 1L)))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val updated = gson.fromJson(response.bodyAsText(), Genre::class.java)
        assertEquals("NewName", updated.name)

        cleanup()
    }

    @Test
    fun `DELETE api genre id returns 204`() = testApplication {
        setupLibraryApp()
        val token = createToken(userId = 1L, role = Role.USER)

        val createResponse = client.post("/api/genre") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(Genre(name = "ToDelete", userId = 1L)))
        }
        val created = gson.fromJson(createResponse.bodyAsText(), Genre::class.java)

        val response = client.delete("/api/genre/${created.id}") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)

        cleanup()
    }

    @Test
    fun `GET api genre returns 401 without token`() = testApplication {
        setupLibraryApp()
        val response = client.get("/api/genre")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
