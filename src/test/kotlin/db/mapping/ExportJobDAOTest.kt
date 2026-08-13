package db.mapping

import com.dw.db.mapping.ExportJobDAO
import com.dw.db.mapping.ExportJobTable
import com.dw.db.mapping.UserDAO
import com.dw.model.dto.ExportJobStatus
import com.dw.plugins.configureDatabases
import io.ktor.server.config.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*

class ExportJobDAOTest {

    private val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:export_job_dao_test;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver"
    )

    @BeforeTest
    fun setup() {
        configureDatabases(testConfig)
    }

    @AfterTest
    fun tearDown() {
        transaction { exec("DROP ALL OBJECTS") }
    }

    @Test
    fun testCreateDefaultsToPending() {
        transaction {
            val user = UserDAO.new {
                username = "testuser1"
                email = "user1@example.com"
                password = "hashedpass"
                role = "USER"
                salt = "salt"
            }
            val dao = ExportJobDAO.new {
                userId = user.id.value
                createdOn = "2026-08-06T00:00:00"
            }
            assertEquals(ExportJobStatus.PENDING, dao.toDto().status)
        }
    }

    @Test
    fun testTransitionToCompleted() {
        transaction {
            val user = UserDAO.new {
                username = "testuser2"
                email = "user2@example.com"
                password = "hashedpass"
                role = "USER"
                salt = "salt"
            }
            val dao = ExportJobDAO.new {
                userId = user.id.value
                createdOn = "2026-08-06T00:00:00"
            }
            dao.status = ExportJobStatus.COMPLETED.name
            dao.filePath = "/exports/export_1_123.zip"
            dao.completedOn = "2026-08-06T00:01:00"

            val reloaded = ExportJobDAO.findById(dao.id)!!
            assertEquals(ExportJobStatus.COMPLETED, reloaded.toDto().status)
            assertEquals("/exports/export_1_123.zip", reloaded.filePath)
        }
    }

    @Test
    fun testFindByUserId() {
        transaction {
            val user1 = UserDAO.new {
                username = "testuser10"
                email = "user10@example.com"
                password = "hashedpass"
                role = "USER"
                salt = "salt"
            }
            val user2 = UserDAO.new {
                username = "testuser20"
                email = "user20@example.com"
                password = "hashedpass"
                role = "USER"
                salt = "salt"
            }
            ExportJobDAO.new { userId = user1.id.value; createdOn = "2026-08-06T00:00:00" }
            ExportJobDAO.new { userId = user2.id.value; createdOn = "2026-08-06T00:00:00" }

            val jobsForUser1 = ExportJobDAO.find { ExportJobTable.userId eq user1.id.value }
            assertEquals(1, jobsForUser1.count())
        }
    }
}
