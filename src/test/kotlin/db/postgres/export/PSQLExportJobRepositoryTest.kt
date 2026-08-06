package db.postgres.export

import com.dw.db.mapping.UserDAO
import com.dw.db.postgres.export.PSQLExportJobRepository
import com.dw.model.dto.ExportJobStatus
import com.dw.model.dto.Role
import com.dw.plugins.configureDatabases
import io.ktor.server.config.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*

class PSQLExportJobRepositoryTest {

    private val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:repo_export_test;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver"
    )

    private val repository = PSQLExportJobRepository()

    @BeforeTest
    fun setup() {
        configureDatabases(testConfig)
    }

    @AfterTest
    fun tearDown() {
        transaction { exec("DROP ALL OBJECTS") }
    }

    // export_job.user_id is FK-constrained (see V4 migration), so a user row must exist
    // before an export job can reference it.
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

    @Test
    fun `create starts a job in PENDING and stores expiresOn`() = runBlocking {
        createUser(1L)
        val job = repository.create(1L, expiresOn = "2026-08-07T00:00:00")
        assertEquals(ExportJobStatus.PENDING, job.status)
        assertEquals("2026-08-07T00:00:00", repository.getExpiresOn(job.id))
    }

    @Test
    fun `markRunning then markCompleted transitions status and stores file path`() = runBlocking {
        createUser(1L)
        val job = repository.create(1L, expiresOn = "2026-08-07T00:00:00")
        repository.markRunning(job.id)
        assertEquals(ExportJobStatus.RUNNING, repository.findById(job.id)?.status)

        repository.markCompleted(job.id, "/exports/export_1_123.zip")
        val completed = repository.findById(job.id)
        assertEquals(ExportJobStatus.COMPLETED, completed?.status)
        assertEquals("/exports/export_1_123.zip", repository.getFilePath(job.id))
        assertNotNull(completed?.completedOn)
        Unit
    }

    @Test
    fun `markFailed records the error`() = runBlocking {
        createUser(1L)
        val job = repository.create(1L, expiresOn = "2026-08-07T00:00:00")
        repository.markFailed(job.id, "disk full")

        val failed = repository.findById(job.id)
        assertEquals(ExportJobStatus.FAILED, failed?.status)
        assertEquals("disk full", failed?.error)
    }

    @Test
    fun `findByIdAndUserId returns null for a different user`() = runBlocking {
        createUser(1L)
        val job = repository.create(1L, expiresOn = "2026-08-07T00:00:00")
        assertNull(repository.findByIdAndUserId(job.id, 2L))
        assertNotNull(repository.findByIdAndUserId(job.id, 1L))
        Unit
    }

    @Test
    fun `findAllByUserId lists only that user's jobs`() = runBlocking {
        createUser(1L)
        createUser(2L)
        repository.create(1L, expiresOn = "2026-08-07T00:00:00")
        repository.create(1L, expiresOn = "2026-08-07T00:00:00")
        repository.create(2L, expiresOn = "2026-08-07T00:00:00")

        assertEquals(2, repository.findAllByUserId(1L).size)
        assertEquals(1, repository.findAllByUserId(2L).size)
    }
}
