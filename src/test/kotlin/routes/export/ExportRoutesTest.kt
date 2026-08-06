package routes.export

import com.dw.db.mapping.UserDAO
import com.dw.model.dto.ExportJob
import com.dw.model.dto.ExportJobStatus
import com.dw.model.dto.Role
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.coroutines.delay
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import routes.BaseRouteTest
import kotlin.test.*

class ExportRoutesTest : BaseRouteTest() {

    override val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:export_route_test;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver",
        "ktor.jwt.secret" to "secret",
        "ktor.jwt.issuer" to "issuer",
        "ktor.jwt.audience" to "audience",
        "ktor.jwt.realm" to "realm",
        "ktor.export.directory" to "${System.getProperty("java.io.tmpdir")}/export-route-test-${System.nanoTime()}",
        "ktor.export.retentionHours" to "24"
    )

    // export_job.user_id has a FK to "user"("id") (see V4 migration), so any test that
    // persists an export job needs a matching user row first (mirrors PreferenceRoutesTest).
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

    private suspend fun ApplicationTestBuilder.waitForCompletion(id: Long, token: String, timeoutMs: Long = 5000): String {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            val response = client.get("/api/exports/$id") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            val job = gson.fromJson(response.bodyAsText(), ExportJob::class.java)
            if (job.status == ExportJobStatus.COMPLETED || job.status == ExportJobStatus.FAILED) {
                return job.status.name
            }
            delay(50)
        }
        throw AssertionError("job $id did not finish within ${timeoutMs}ms")
    }

    @Test
    fun `POST api exports returns 202 with a PENDING job`() = testApplication {
        setupLibraryApp()
        client.get("/") // force app start
        createUser(1L)
        val token = createToken(userId = 1L, role = Role.USER)

        val response = client.post("/api/exports") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.Accepted, response.status)
        val job = gson.fromJson(response.bodyAsText(), ExportJob::class.java)
        assertEquals(ExportJobStatus.PENDING, job.status)

        cleanup()
    }

    @Test
    fun `export job eventually completes and can be downloaded`() = testApplication {
        setupLibraryApp()
        client.get("/") // force app start
        createUser(1L)
        val token = createToken(userId = 1L, role = Role.USER)

        val createResponse = client.post("/api/exports") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val job = gson.fromJson(createResponse.bodyAsText(), ExportJob::class.java)

        val finalStatus = waitForCompletion(job.id, token)
        assertEquals("COMPLETED", finalStatus)

        val downloadResponse = client.get("/api/exports/${job.id}/download") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, downloadResponse.status)
        assertNotNull(downloadResponse.headers[HttpHeaders.ContentDisposition])

        cleanup()
    }

    @Test
    fun `download before job completes returns 409`() = testApplication {
        setupLibraryApp()
        client.get("/") // force app start
        createUser(1L)
        val token = createToken(userId = 1L, role = Role.USER)

        val createResponse = client.post("/api/exports") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val job = gson.fromJson(createResponse.bodyAsText(), ExportJob::class.java)

        val downloadResponse = client.get("/api/exports/${job.id}/download") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.Conflict, downloadResponse.status)

        cleanup()
    }

    @Test
    fun `GET api exports id returns 404 for another user's job`() = testApplication {
        setupLibraryApp()
        client.get("/") // force app start
        createUser(1L)
        createUser(2L)
        val tokenA = createToken(userId = 1L, role = Role.USER)
        val tokenB = createToken(userId = 2L, role = Role.USER)

        val createResponse = client.post("/api/exports") {
            header(HttpHeaders.Authorization, "Bearer $tokenA")
        }
        val job = gson.fromJson(createResponse.bodyAsText(), ExportJob::class.java)

        val response = client.get("/api/exports/${job.id}") {
            header(HttpHeaders.Authorization, "Bearer $tokenB")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)

        cleanup()
    }

    @Test
    fun `GET api exports returns 401 without token`() = testApplication {
        setupLibraryApp()
        val response = client.get("/api/exports")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
