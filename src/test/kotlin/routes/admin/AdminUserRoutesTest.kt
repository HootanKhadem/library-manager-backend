package com.dw.routes.admin

import com.dw.model.dto.Role
import com.dw.model.dto.UserDTO
import com.dw.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.junit.Test
import routes.BaseRouteTest
import kotlin.test.assertEquals

class AdminUserRoutesTest : BaseRouteTest() {

    override val testConfig = MapApplicationConfig(
        "ktor.jwt.secret" to "secret",
        "ktor.jwt.issuer" to "issuer",
        "ktor.jwt.audience" to "audience",
        "ktor.jwt.realm" to "realm",
        "ktor.psql-database.url" to "jdbc:h2:mem:test_user_dao;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver"
    )

    @Test
    fun testAdminCanCreateUser() = testApplication {
        setupLibraryApp()

        val adminToken = createToken(email = "admin@example.com", role = Role.ADMIN)
        val newUser = UserDTO(name = "New User", email = "new@example.com", password = "password", role = Role.USER)

        val response = client.post("/admin/users") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody(gson.toJson(newUser))
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun testRegularUserCannotCreateUser() = testApplication {
        setupLibraryApp()

        val userToken = createToken(email = "user@example.com", role = Role.USER)
        val newUser = UserDTO(name = "New User", email = "new@example.com", password = "password", role = Role.USER)

        val response = client.post("/admin/users") {
            header(HttpHeaders.Authorization, "Bearer $userToken")
            contentType(ContentType.Application.Json)
            setBody(gson.toJson(newUser))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun testTokenWithInvalidIssuerCannotCreateUser() = testApplication {
        setupLibraryApp()

        val token = createToken(issuer = "external-issuer")
        val newUser = UserDTO(name = "New User", email = "new@example.com", password = "password", role = Role.USER)

        val response = client.post("/admin/users") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(gson.toJson(newUser))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testTokenWithInvalidAudienceCannotCreateUser() = testApplication {
        setupLibraryApp()

        val token = createToken(audience = "external-audience")
        val newUser = UserDTO(name = "New User", email = "new@example.com", password = "password", role = Role.USER)

        val response = client.post("/admin/users") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(gson.toJson(newUser))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testTokenWithMissingRoleClaimCannotCreateUser() = testApplication {
        setupLibraryApp()

        val token = createToken(role = null)
        val newUser = UserDTO(name = "New User", email = "new@example.com", password = "password", role = Role.USER)

        val response = client.post("/admin/users") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(gson.toJson(newUser))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun testUnauthenticatedCannotCreateUser() = testApplication {
        setupLibraryApp()

        val newUser = UserDTO(name = "New User", email = "new@example.com", password = "password", role = Role.USER)

        val response = client.post("/admin/users") {
            contentType(ContentType.Application.Json)
            setBody(gson.toJson(newUser))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
