package routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.dw.db.mapping.AuthorTable
import com.dw.db.mapping.BookTable
import com.dw.db.mapping.UserTable
import com.dw.model.dto.Role
import com.dw.plugins.*
import com.google.gson.Gson
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*

open class BaseRouteTest {
    protected open val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:test_db;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver",
        "ktor.jwt.secret" to "secret",
        "ktor.jwt.issuer" to "issuer",
        "ktor.jwt.audience" to "audience",
        "ktor.jwt.realm" to "realm"
    )

    protected val gson = Gson()

    protected fun cleanup() {
        transaction {
            SchemaUtils.drop(BookTable, AuthorTable, UserTable)
        }
    }

    protected fun createToken(
        email: String? = "test@example.com",
        role: Role? = Role.USER,
        issuer: String = "issuer",
        audience: String = "audience",
        secret: String = "secret"
    ): String {
        var builder = JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000))

        if (email != null) {
            builder = builder.withClaim("email", email)
        }

        if (role != null) {
            builder = builder.withClaim("role", role.name)
        }

        return builder.sign(Algorithm.HMAC256(secret))
    }

    protected fun ApplicationTestBuilder.setupLibraryApp() {
        environment { config = testConfig }
        application {
            configureLibraryModule()
        }
    }

    private suspend fun Application.configureLibraryModule() {
        configureDatabases(testConfig)
        configureDependencyInjection()
        configureContentNegotiation()
        configureJWT()
        configurePublicRouting()
        configureAuthenticatedRouting()
        configureAdminRouting()
    }
}
