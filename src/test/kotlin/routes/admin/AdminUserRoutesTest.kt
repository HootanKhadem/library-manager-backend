package com.dw.routes.admin

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
        val token = jwtService.generateToken(adminUser)

        val newUser = UserDTO(name = "New User", email = "new@example.com", password = "password", role = Role.USER)

        val response = client.post("/admin/users") {
            header(HttpHeaders.Authorization, "Bearer $token")
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
        val token = jwtService.generateToken(regularUser)

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
