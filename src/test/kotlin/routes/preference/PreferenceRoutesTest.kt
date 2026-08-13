package routes.preference

import com.dw.db.mapping.UserDAO
import com.dw.model.dto.Role
import com.dw.model.dto.UserPreference
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import routes.BaseRouteTest
import kotlin.test.*

class PreferenceRoutesTest : BaseRouteTest() {

    override val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:pref_route_test;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver",
        "ktor.jwt.secret" to "secret",
        "ktor.jwt.issuer" to "issuer",
        "ktor.jwt.audience" to "audience",
        "ktor.jwt.realm" to "realm"
    )

    // user_preference.user_id has a FK to "user"("id") (see V4 migration), so any
    // test that persists a preference row needs a matching user row first.
    private fun createUser(userId: Long) {
        transaction {
            UserDAO.new {
                username = "user$userId"
                password = "password"
                email = "user$userId@example.com"
                role = Role.USER.name
                salt = "salt"
            }
        }
    }

    @Test
    fun `GET api preferences returns defaults when unset`() = testApplication {
        setupLibraryApp()
        val token = createToken(userId = 1L, role = Role.USER)

        val response = client.get("/api/preferences") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val prefs = gson.fromJson(response.bodyAsText(), UserPreference::class.java)
        assertEquals(30, prefs.defaultLoanDurationDays)
        assertEquals("en", prefs.language)

        cleanup()
    }

    @Test
    fun `PUT api preferences saves and GET reflects it`() = testApplication {
        setupLibraryApp()
        client.get("/") // force app start
        createUser(1L)
        val token = createToken(userId = 1L, role = Role.USER)

        val putResponse = client.put("/api/preferences") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(UserPreference(libraryName = "My Personal Library", ownerName = "Bibliophile")))
        }
        assertEquals(HttpStatusCode.OK, putResponse.status)

        val getResponse = client.get("/api/preferences") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val prefs = gson.fromJson(getResponse.bodyAsText(), UserPreference::class.java)
        assertEquals("My Personal Library", prefs.libraryName)
        assertEquals("Bibliophile", prefs.ownerName)

        cleanup()
    }

    @Test
    fun `PUT api preferences rejects invalid language with 400`() = testApplication {
        setupLibraryApp()
        client.get("/") // force app start
        createUser(1L)
        val token = createToken(userId = 1L, role = Role.USER)

        val response = client.put("/api/preferences") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(UserPreference(language = "de")))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)

        cleanup()
    }

    @Test
    fun `preferences are isolated per user`() = testApplication {
        setupLibraryApp()
        client.get("/") // force app start
        createUser(1L)
        createUser(2L)
        val tokenA = createToken(userId = 1L, role = Role.USER)
        val tokenB = createToken(userId = 2L, role = Role.USER)

        val putResponseA = client.put("/api/preferences") {
            header(HttpHeaders.Authorization, "Bearer $tokenA")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(UserPreference(libraryName = "User A's Library")))
        }
        assertEquals(HttpStatusCode.OK, putResponseA.status)

        val responseB = client.get("/api/preferences") {
            header(HttpHeaders.Authorization, "Bearer $tokenB")
        }
        val prefsB = gson.fromJson(responseB.bodyAsText(), UserPreference::class.java)
        assertNull(prefsB.libraryName)

        cleanup()
    }

    @Test
    fun `GET api preferences returns 401 without token`() = testApplication {
        setupLibraryApp()
        val response = client.get("/api/preferences")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
