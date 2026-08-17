package routes.user

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.dw.db.mapping.UserDAO
import com.dw.model.dto.Role
import com.dw.service.util.PasswordUtil
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import routes.BaseRouteTest
import java.util.*
import kotlin.test.*

class AuthenticationRoutesTest : BaseRouteTest() {

    override val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:auth_route_test;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver",
        "ktor.jwt.secret" to "secret",
        "ktor.jwt.issuer" to "issuer",
        "ktor.jwt.audience" to "audience",
        "ktor.jwt.realm" to "realm"
    )

    private fun createUser(email: String, rawPassword: String, role: Role = Role.USER) {
        val userSalt = PasswordUtil.generateSalt()
        transaction {
            UserDAO.new {
                username = email.substringBefore("@")
                password = PasswordUtil.hashWithSalt(rawPassword, userSalt)
                this.email = email
                this.role = role.name
                salt = userSalt
            }
        }
    }

    private fun setCookiesOf(response: io.ktor.client.statement.HttpResponse) =
        response.headers.getAll(HttpHeaders.SetCookie).orEmpty().map { parseServerSetCookieHeader(it) }

    @Test
    fun `login sets httpOnly SameSite=None cookies and returns no raw tokens in the body`() = testApplication {
        setupLibraryApp()
        startApplication()
        createUser("test@example.com", "password123")

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(gson.toJson(mapOf("email" to "test@example.com", "password" to "password123")))
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val body = gson.fromJson(response.bodyAsText(), Map::class.java)
        assertEquals("test@example.com", body["email"])
        assertEquals("USER", body["role"])
        assertFalse(body.containsKey("access_token"))
        assertFalse(body.containsKey("refresh_token"))

        val cookies = setCookiesOf(response)
        val accessCookie = cookies.first { it.name == "access_token" }
        val refreshCookie = cookies.first { it.name == "refresh_token" }
        for (authCookie in listOf(accessCookie, refreshCookie)) {
            assertTrue(authCookie.httpOnly)
            assertTrue(authCookie.secure)
            assertEquals("None", authCookie.extensions["SameSite"])
            assertEquals("/", authCookie.path)
        }
        assertNotEquals(accessCookie.value, refreshCookie.value)

        cleanup()
    }

    @Test
    fun `logout clears both auth cookies and works with no existing session`() = testApplication {
        setupLibraryApp()

        val response = client.post("/auth/logout")

        assertEquals(HttpStatusCode.OK, response.status)
        val cookies = setCookiesOf(response)
        val accessCookie = cookies.first { it.name == "access_token" }
        val refreshCookie = cookies.first { it.name == "refresh_token" }
        assertEquals("", accessCookie.value)
        assertEquals("", refreshCookie.value)
        assertNotNull(accessCookie.expires)
        assertTrue(accessCookie.expires!!.timestamp < System.currentTimeMillis())
        assertNotNull(refreshCookie.expires)
        assertTrue(refreshCookie.expires!!.timestamp < System.currentTimeMillis())
    }

    @Test
    fun `logout clears cookies even when the caller sends an expired access_token`() = testApplication {
        setupLibraryApp()
        val expiredToken = JWT.create()
            .withAudience("audience")
            .withIssuer("issuer")
            .withClaim("email", "test@example.com")
            .withClaim("role", "USER")
            .withClaim("userId", 1L)
            .withClaim("type", "access")
            .withExpiresAt(Date(System.currentTimeMillis() - 1000))
            .sign(Algorithm.HMAC256("secret"))

        val response = client.post("/auth/logout") {
            cookie("access_token", expiredToken)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val cookies = setCookiesOf(response)
        assertTrue(cookies.any { it.name == "access_token" && it.value == "" })
        assertTrue(cookies.any { it.name == "refresh_token" && it.value == "" })
    }
}
