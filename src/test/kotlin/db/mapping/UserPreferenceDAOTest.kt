package db.mapping

import com.dw.db.mapping.UserDAO
import com.dw.db.mapping.UserPreferenceDAO
import com.dw.db.mapping.UserPreferenceTable
import com.dw.model.dto.UserPreference
import com.dw.plugins.configureDatabases
import io.ktor.server.config.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*

class UserPreferenceDAOTest {

    private val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:user_pref_dao_test;DB_CLOSE_DELAY=-1",
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
    fun testCreateAndReadDefaults() {
        transaction {
            UserDAO.new {
                username = "testuser1"
                email = "user1@example.com"
                password = "hashedpass"
                role = "USER"
                salt = "salt"
            }
            val dao = UserPreferenceDAO.new {
                userId = 1L
            }
            val dto = dao.toDto()
            assertEquals(30, dto.defaultLoanDurationDays)
            assertEquals("DD MMM YYYY", dto.dateFormat)
            assertEquals("en", dto.language)
            assertNull(dto.libraryName)
        }
    }

    @Test
    fun testUpdateFromDto() {
        transaction {
            UserDAO.new {
                username = "testuser2"
                email = "user2@example.com"
                password = "hashedpass"
                role = "USER"
                salt = "salt"
            }
            val dao = UserPreferenceDAO.new { userId = 2L }
            val update = UserPreference(
                libraryName = "My Personal Library",
                ownerName = "Bibliophile",
                description = "A curated collection.",
                defaultLoanDurationDays = 14,
                dateFormat = "YYYY-MM-DD",
                language = "fa",
                modifiedOn = "2026-08-06T00:00:00"
            )
            dao.updateFromDto(update, actingUserId = 2L)

            val reloaded = UserPreferenceDAO.findById(dao.id)!!.toDto()
            assertEquals("My Personal Library", reloaded.libraryName)
            assertEquals("Bibliophile", reloaded.ownerName)
            assertEquals(14, reloaded.defaultLoanDurationDays)
            assertEquals("fa", reloaded.language)
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
            UserPreferenceDAO.new { userId = user1.id.value; libraryName = "Lib A" }
            UserPreferenceDAO.new { userId = user2.id.value; libraryName = "Lib B" }

            val found = UserPreferenceDAO.find { UserPreferenceTable.userId eq user1.id.value }.firstOrNull()
            assertNotNull(found)
            assertEquals("Lib A", found.libraryName)
        }
    }
}
