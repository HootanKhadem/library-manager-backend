package service.preference

import com.dw.db.mapping.UserDAO
import com.dw.db.postgres.preference.PSQLUserPreferenceRepository
import com.dw.model.dto.UserPreference
import com.dw.plugins.configureDatabases
import com.dw.service.preference.UserPreferenceServiceImpl
import io.ktor.server.config.MapApplicationConfig
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*

class UserPreferenceServiceTest {

    private val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:pref_service_test;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver"
    )

    private val repository = PSQLUserPreferenceRepository()
    private val service = UserPreferenceServiceImpl(repository)

    @BeforeTest
    fun setup() {
        configureDatabases(testConfig)
        // Create test users for foreign key constraint
        transaction {
            UserDAO.new {
                username = "testuser1"
                email = "user1@example.com"
                password = "hashedpass"
                role = "USER"
                salt = "salt"
            }
        }
    }

    @AfterTest
    fun tearDown() {
        transaction { exec("DROP ALL OBJECTS") }
    }

    @Test
    fun `getPreferences returns schema defaults when nothing saved yet`(): Unit = runBlocking {
        val prefs = service.getPreferences(1L)
        assertEquals(30, prefs.defaultLoanDurationDays)
        assertEquals("en", prefs.language)
        assertNull(prefs.libraryName)
    }

    @Test
    fun `savePreferences persists and getPreferences returns it back`(): Unit = runBlocking {
        service.savePreferences(1L, UserPreference(libraryName = "My Personal Library", language = "fa"))

        val prefs = service.getPreferences(1L)
        assertEquals("My Personal Library", prefs.libraryName)
        assertEquals("fa", prefs.language)
    }

    @Test
    fun `savePreferences rejects non-positive loan duration`(): Unit = runBlocking {
        assertFailsWith<IllegalArgumentException> {
            service.savePreferences(1L, UserPreference(defaultLoanDurationDays = 0))
        }
    }

    @Test
    fun `savePreferences rejects unknown language`(): Unit = runBlocking {
        assertFailsWith<IllegalArgumentException> {
            service.savePreferences(1L, UserPreference(language = "de"))
        }
    }

    @Test
    fun `savePreferences rejects unknown date format`(): Unit = runBlocking {
        assertFailsWith<IllegalArgumentException> {
            service.savePreferences(1L, UserPreference(dateFormat = "not-a-format"))
        }
    }
}
