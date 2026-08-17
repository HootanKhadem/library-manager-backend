package routes.user

import com.dw.db.postgres.user.PSQLUserRepository
import com.dw.model.dto.LoginDTO
import com.dw.model.dto.Role
import com.dw.model.dto.UserDTO
import com.dw.service.util.PasswordUtil
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import routes.BaseRouteTest
import kotlin.test.Test
import kotlin.test.assertEquals

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

    @Test
    fun `POST auth login with wrong password returns 401`() = testApplication {
        setupLibraryApp()
        startApplication()

        val email = "test@example.com"
        val salt = PasswordUtil.generateSalt()
        runBlocking {
            PSQLUserRepository().save(
                UserDTO(
                    name = "testuser",
                    email = email,
                    password = PasswordUtil.hashWithSalt("correct-password", salt),
                    salt = salt,
                    role = Role.USER
                )
            )
        }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(gson.toJson(LoginDTO(email = email, password = "wrong-password")))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
