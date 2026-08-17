package db.postgres.preference

import com.dw.db.mapping.UserDAO
import com.dw.db.mapping.UserTable
import com.dw.db.postgres.preference.PSQLUserPreferenceRepository
import com.dw.model.dto.Role
import com.dw.model.dto.UserPreference
import com.dw.plugins.configureDatabases
import io.ktor.server.config.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*

class PSQLUserPreferenceRepositoryTest {

    private val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:repo_pref_test;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver"
    )

    private val repository = PSQLUserPreferenceRepository()

    @BeforeTest
    fun setup() {
        configureDatabases(testConfig)
    }

    @AfterTest
    fun tearDown() {
        transaction { exec("DROP ALL OBJECTS") }
    }

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
    fun `findByUserId returns null when no row exists`() = runBlocking {
        createUser(999L)
        assertNull(repository.findByUserId(999L))
    }

    @Test
    fun `upsert creates a row on first call`() = runBlocking {
        createUser(1L)
        val saved = repository.upsert(1L, UserPreference(libraryName = "First Library"))
        assertEquals("First Library", saved.libraryName)
        assertEquals("First Library", repository.findByUserId(1L)?.libraryName)
    }

    @Test
    fun `upsert updates existing row instead of creating a duplicate`() = runBlocking {
        createUser(1L)
        repository.upsert(1L, UserPreference(libraryName = "Original"))
        repository.upsert(1L, UserPreference(libraryName = "Renamed"))

        assertEquals("Renamed", repository.findByUserId(1L)?.libraryName)
    }

    @Test
    fun `upsert scopes rows per user`() = runBlocking {
        createUser(1L)
        createUser(2L)
        repository.upsert(1L, UserPreference(libraryName = "User 1 Library"))
        repository.upsert(2L, UserPreference(libraryName = "User 2 Library"))

        assertEquals("User 1 Library", repository.findByUserId(1L)?.libraryName)
        assertEquals("User 2 Library", repository.findByUserId(2L)?.libraryName)
    }

    @Test
    fun `seedDefaults creates a row with schema default values`() = runBlocking {
        createUser(1L)
        repository.seedDefaults(1L)

        val prefs = repository.findByUserId(1L)
        assertNotNull(prefs)
        assertNull(prefs.libraryName)
        assertNull(prefs.ownerName)
        assertNull(prefs.description)
        assertEquals(30, prefs.defaultLoanDurationDays)
        assertEquals("DD MMM YYYY", prefs.dateFormat)
        assertEquals("en", prefs.language)
    }
}
