package routes.user

import com.dw.db.mapping.UserDAO
import com.dw.model.dto.Role
import com.dw.service.util.PasswordUtil
import com.sun.net.httpserver.HttpServer
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import routes.BaseRouteTest
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.*

/** End to end: HTTP route -> service -> ResendEmailSender -> stubbed Resend HTTP server. */
class PasswordResetRoutesTest : BaseRouteTest() {

    private val received = CopyOnWriteArrayList<Pair<String?, String>>()
    private lateinit var resend: HttpServer

    override val testConfig by lazy {
        MapApplicationConfig(
            "ktor.psql-database.url" to "jdbc:h2:mem:reset_route_test;DB_CLOSE_DELAY=-1",
            "ktor.psql-database.username" to "sa",
            "ktor.psql-database.password" to "",
            "ktor.psql-database.driver" to "org.h2.Driver",
            "ktor.jwt.secret" to "secret",
            "ktor.jwt.issuer" to "issuer",
            "ktor.jwt.audience" to "audience",
            "ktor.jwt.realm" to "realm",
            "ktor.frontendUrl" to "https://app.example.com",
            "ktor.email.resendApiKey" to "re_test",
            "ktor.email.resendApiUrl" to "http://127.0.0.1:${resend.address.port}/emails",
            "ktor.email.from" to "noreply@example.com"
        )
    }

    @BeforeTest
    fun startStub() {
        received.clear()
        resend = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        resend.createContext("/emails") { ex ->
            received += ex.requestHeaders.getFirst("Authorization") to ex.requestBody.readBytes().decodeToString()
            val resp = """{"id":"1"}"""
            ex.sendResponseHeaders(200, resp.length.toLong())
            ex.responseBody.use { it.write(resp.toByteArray()) }
        }
        resend.start()
    }

    @AfterTest
    fun stopStub() {
        resend.stop(0)
        cleanup()
    }

    private fun createUser(email: String, rawPassword: String) {
        val userSalt = PasswordUtil.generateSalt()
        transaction {
            UserDAO.new {
                username = email.substringBefore("@")
                password = PasswordUtil.hashWithSalt(rawPassword, userSalt)
                this.email = email
                role = Role.USER.name
                salt = userSalt
            }
        }
    }

    private fun json(vararg pairs: Pair<String, String>) = gson.toJson(mapOf(*pairs))

    private fun tokenFromStub(): String =
        Regex("token=([A-Za-z0-9_-]+)").find(received.last().second)!!.groupValues[1]

    @Test
    fun `forgot then reset then login with new password`() = testApplication {
        setupLibraryApp()
        startApplication()
        createUser("alice@example.com", "OldPassword1")

        val forgot = client.post("/auth/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody(json("email" to "alice@example.com"))
        }
        assertEquals(HttpStatusCode.OK, forgot.status)
        assertEquals("Bearer re_test", received.single().first)
        assertTrue(received.single().second.contains("https://app.example.com/reset-password?token="))
        assertTrue(received.single().second.contains("alice@example.com"))

        val reset = client.post("/auth/reset-password") {
            contentType(ContentType.Application.Json)
            setBody(json("token" to tokenFromStub(), "newPassword" to "NewPassword1"))
        }
        assertEquals(HttpStatusCode.OK, reset.status)

        val oldLogin = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(json("email" to "alice@example.com", "password" to "OldPassword1"))
        }
        assertNotEquals(HttpStatusCode.OK, oldLogin.status)

        val newLogin = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(json("email" to "alice@example.com", "password" to "NewPassword1"))
        }
        assertEquals(HttpStatusCode.OK, newLogin.status)
    }

    @Test
    fun `forgot-password for unknown email returns identical 200 and sends no email`() = testApplication {
        setupLibraryApp()
        startApplication()
        createUser("alice@example.com", "OldPassword1")

        suspend fun forgot(email: String) = client.post("/auth/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody(json("email" to email))
        }
        val known = forgot("alice@example.com")
        received.clear()
        val unknown = forgot("ghost@example.com")

        assertEquals(HttpStatusCode.OK, unknown.status)
        assertEquals(known.bodyAsText(), unknown.bodyAsText())
        assertTrue(received.isEmpty())
    }

    @Test
    fun `reset-password with bad token returns 400`() = testApplication {
        setupLibraryApp()
        startApplication()

        val response = client.post("/auth/reset-password") {
            contentType(ContentType.Application.Json)
            setBody(json("token" to "bogus", "newPassword" to "NewPassword1"))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `reset-password with weak password returns 400 and token is reusable`() = testApplication {
        setupLibraryApp()
        startApplication()
        createUser("alice@example.com", "OldPassword1")
        client.post("/auth/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody(json("email" to "alice@example.com"))
        }
        val token = tokenFromStub()

        val weak = client.post("/auth/reset-password") {
            contentType(ContentType.Application.Json)
            setBody(json("token" to token, "newPassword" to "weak"))
        }
        assertEquals(HttpStatusCode.BadRequest, weak.status)

        val ok = client.post("/auth/reset-password") {
            contentType(ContentType.Application.Json)
            setBody(json("token" to token, "newPassword" to "NewPassword1"))
        }
        assertEquals(HttpStatusCode.OK, ok.status)
    }

    @Test
    fun `token cannot be used twice`() = testApplication {
        setupLibraryApp()
        startApplication()
        createUser("alice@example.com", "OldPassword1")
        client.post("/auth/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody(json("email" to "alice@example.com"))
        }
        val token = tokenFromStub()

        repeat(2) { i ->
            val r = client.post("/auth/reset-password") {
                contentType(ContentType.Application.Json)
                setBody(json("token" to token, "newPassword" to "NewPassword1"))
            }
            assertEquals(if (i == 0) HttpStatusCode.OK else HttpStatusCode.BadRequest, r.status)
        }
    }
}
