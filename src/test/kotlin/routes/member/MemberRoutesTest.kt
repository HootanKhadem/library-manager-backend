package routes.member

import com.dw.model.dto.Member
import com.dw.model.dto.Role
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import routes.BaseRouteTest
import kotlin.test.*

class MemberRoutesTest : BaseRouteTest() {

    override val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:member_route_test;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver",
        "ktor.jwt.secret" to "secret",
        "ktor.jwt.issuer" to "issuer",
        "ktor.jwt.audience" to "audience",
        "ktor.jwt.realm" to "realm"
    )

    private fun memberPayload(userId: Long? = null) = Member(
        name = "Lucas M.",
        email = "lucas@example.com",
        password = "password123",
        userId = userId
    )

    @Test
    fun `POST api member creates member and returns 201`() = testApplication {
        setupLibraryApp()
        val token = createToken(userId = 1L, role = Role.USER)

        val response = client.post("/api/member") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(memberPayload()))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val created = gson.fromJson(response.bodyAsText(), Member::class.java)
        assertNotNull(created.id)
        assertEquals("Lucas M.", created.name)

        cleanup()
    }

    @Test
    fun `GET api member returns members for authenticated user`() = testApplication {
        setupLibraryApp()
        val token = createToken(userId = 2L, role = Role.USER)

        client.post("/api/member") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(memberPayload(userId = 2L)))
        }

        val response = client.get("/api/member") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val members = gson.fromJson(response.bodyAsText(), Array<Member>::class.java)
        assertTrue(members.isNotEmpty())

        cleanup()
    }

    @Test
    fun `DELETE api member id returns 204`() = testApplication {
        setupLibraryApp()
        val token = createToken(userId = 1L, role = Role.USER)

        val createResponse = client.post("/api/member") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(memberPayload()))
        }
        val created = gson.fromJson(createResponse.bodyAsText(), Member::class.java)

        val response = client.delete("/api/member/${created.id}") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)

        cleanup()
    }

    @Test
    fun `GET api member returns 401 without token`() = testApplication {
        setupLibraryApp()
        val response = client.get("/api/member")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
