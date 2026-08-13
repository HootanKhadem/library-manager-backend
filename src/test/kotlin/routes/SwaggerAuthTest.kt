package routes

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SwaggerAuthTest : BaseRouteTest() {
    override val testConfig: MapApplicationConfig = MapApplicationConfig(
        // DB & JWT (from BaseRouteTest) + Swagger settings
        "ktor.psql-database.url" to "jdbc:h2:mem:test_db;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver",
        "ktor.jwt.secret" to "secret",
        "ktor.jwt.issuer" to "issuer",
        "ktor.jwt.audience" to "audience",
        "ktor.jwt.realm" to "realm",
        "ktor.swagger.path" to "/docs",
        "ktor.swagger.username" to "u",
        "ktor.swagger.password" to "p",
        "ktor.swagger.openapi.file" to "openapi/documentation.yaml",
    )

    @Test
    fun `docs require basic auth`() = testApplication {
        setupLibraryApp()

        val res = client.get("/docs")
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun `docs succeed with valid basic auth`() = testApplication {
        setupLibraryApp()

        val auth = "u:p".encodeToByteArray()
        val basic = java.util.Base64.getEncoder().encodeToString(auth)

        val res = client.get("/docs") {
            header(HttpHeaders.Authorization, "Basic $basic")
        }
        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.bodyAsText()
        assertTrue(body.contains("Swagger UI"), "Body should contain Swagger UI text")
    }
}
