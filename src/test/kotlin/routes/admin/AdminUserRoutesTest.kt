package com.dw.routes.admin

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.dw.model.dto.Role
import com.dw.model.dto.UserDTO
import com.dw.plugins.*
import com.dw.service.authentication.JwtService
import com.google.gson.Gson
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class AdminUserRoutesTest {

    private val jwtConfig = JwtConfig(
        secret = "secret",
        issuer = "issuer",
        audience = "audience",
        realm = "realm"
    )
    private val jwtService = JwtService(jwtConfig)
    private val gson = Gson()

    private fun createSignedToken(
        secret: String = jwtConfig.secret,
        issuer: String = jwtConfig.issuer,
        audience: String = jwtConfig.audience,
        email: String? = "admin@example.com",
        role: String? = Role.ADMIN.name
    ): String {
        var builder = JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000))

        if (email != null) {
            builder = builder.withClaim("email", email)
        }

        if (role != null) {
            builder = builder.withClaim("role", role)
        }

        return builder.sign(Algorithm.HMAC256(secret))
    }

    private suspend fun Application.testModule() {
        install(ContentNegotiation) {
            gson()
        }
        configureDatabases()
        configureDependencyInjection()
        configureJWT()
        configureAdminRouting()
    }

    @Test
    fun testAdminCanCreateUser() = testApplication {
        environment {
            config = MapApplicationConfig(
                "ktor.jwt.secret" to "secret",
                "ktor.jwt.issuer" to "issuer",
                "ktor.jwt.audience" to "audience",
                "ktor.jwt.realm" to "realm",
                "ktor.psql-database.url" to "jdbc:h2:mem:test_user_dao;DB_CLOSE_DELAY=-1",
                "ktor.psql-database.username" to "sa",
                "ktor.psql-database.password" to "",
                "ktor.psql-database.driver" to "org.h2.Driver"
            )
        }
        application {
            testModule()
        }

        val adminUser =
            UserDTO(id = 1, name = "Admin", email = "admin@example.com", password = "pass", role = Role.ADMIN)
        val tokenPair = jwtService.generateToken(adminUser)

        val newUser = UserDTO(name = "New User", email = "new@example.com", password = "password", role = Role.USER)

        val response = client.post("/admin/users") {
            header(HttpHeaders.Authorization, "Bearer ${tokenPair.first}")
            contentType(ContentType.Application.Json)
            setBody(gson.toJson(newUser))
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun testRegularUserCannotCreateUser() = testApplication {
        environment {
            config = MapApplicationConfig(
                "ktor.jwt.secret" to "secret",
                "ktor.jwt.issuer" to "issuer",
                "ktor.jwt.audience" to "audience",
                "ktor.jwt.realm" to "realm",
                "ktor.psql-database.url" to "jdbc:h2:mem:test_user_dao;DB_CLOSE_DELAY=-1",
                "ktor.psql-database.username" to "sa",
                "ktor.psql-database.password" to "",
                "ktor.psql-database.driver" to "org.h2.Driver"
            )
        }
        application {
            testModule()
        }

        val regularUser =
            UserDTO(id = 1, name = "User", email = "user@example.com", password = "pass", role = Role.USER)
        val tokenPair = jwtService.generateToken(regularUser)

        val newUser = UserDTO(name = "New User", email = "new@example.com", password = "password", role = Role.USER)

        val response = client.post("/admin/users") {
            header(HttpHeaders.Authorization, "Bearer ${tokenPair.first}")
            contentType(ContentType.Application.Json)
            setBody(gson.toJson(newUser))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun testTokenWithInvalidIssuerCannotCreateUser() = testApplication {
        environment {
            config = MapApplicationConfig(
                "ktor.jwt.secret" to "secret",
                "ktor.jwt.issuer" to "issuer",
                "ktor.jwt.audience" to "audience",
                "ktor.jwt.realm" to "realm",
                "ktor.psql-database.url" to "jdbc:h2:mem:test_user_dao;DB_CLOSE_DELAY=-1",
                "ktor.psql-database.username" to "sa",
                "ktor.psql-database.password" to "",
                "ktor.psql-database.driver" to "org.h2.Driver"
            )
        }
        application {
            testModule()
        }

        val token = createSignedToken(issuer = "external-issuer")
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
        environment {
            config = MapApplicationConfig(
                "ktor.jwt.secret" to "secret",
                "ktor.jwt.issuer" to "issuer",
                "ktor.jwt.audience" to "audience",
                "ktor.jwt.realm" to "realm",
                "ktor.psql-database.url" to "jdbc:h2:mem:test_user_dao;DB_CLOSE_DELAY=-1",
                "ktor.psql-database.username" to "sa",
                "ktor.psql-database.password" to "",
                "ktor.psql-database.driver" to "org.h2.Driver"
            )
        }
        application {
            testModule()
        }

        val token = createSignedToken(audience = "external-audience")
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
        environment {
            config = MapApplicationConfig(
                "ktor.jwt.secret" to "secret",
                "ktor.jwt.issuer" to "issuer",
                "ktor.jwt.audience" to "audience",
                "ktor.jwt.realm" to "realm",
                "ktor.psql-database.url" to "jdbc:h2:mem:test_user_dao;DB_CLOSE_DELAY=-1",
                "ktor.psql-database.username" to "sa",
                "ktor.psql-database.password" to "",
                "ktor.psql-database.driver" to "org.h2.Driver"
            )
        }
        application {
            testModule()
        }

        val token = createSignedToken(role = null)
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
        environment {
            config = MapApplicationConfig(
                "ktor.jwt.secret" to "secret",
                "ktor.jwt.issuer" to "issuer",
                "ktor.jwt.audience" to "audience",
                "ktor.jwt.realm" to "realm",
                "ktor.psql-database.url" to "jdbc:h2:mem:test_user_dao;DB_CLOSE_DELAY=-1",
                "ktor.psql-database.username" to "sa",
                "ktor.psql-database.password" to "",
                "ktor.psql-database.driver" to "org.h2.Driver"
            )
        }
        application {
            testModule()
        }

        val newUser = UserDTO(name = "New User", email = "new@example.com", password = "password", role = Role.USER)

        val response = client.post("/admin/users") {
            contentType(ContentType.Application.Json)
            setBody(gson.toJson(newUser))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
