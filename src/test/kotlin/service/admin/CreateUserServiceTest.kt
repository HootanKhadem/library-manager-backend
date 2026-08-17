package service.admin

import com.dw.db.postgres.genre.PSQLGenreRepository
import com.dw.db.postgres.preference.PSQLUserPreferenceRepository
import com.dw.db.postgres.user.PSQLUserRepository
import com.dw.model.dto.Role
import com.dw.model.dto.UserDTO
import com.dw.plugins.configureDatabases
import com.dw.service.admin.CreateUserService
import io.ktor.server.config.MapApplicationConfig
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*

class CreateUserServiceTest {

    private val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:create_user_service_test;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver"
    )

    private val userRepository = PSQLUserRepository()
    private val genreRepository = PSQLGenreRepository()
    private val userPreferenceRepository = PSQLUserPreferenceRepository()
    private val service = CreateUserService(
        userRepository = userRepository,
        genreRepository = genreRepository,
        userPreferenceRepository = userPreferenceRepository
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
    fun `createNewUser seeds default preferences for the new user`() = runBlocking {
        val created = service.createNewUser(
            UserDTO(
                name = "newuser",
                email = "newuser@example.com",
                password = "plaintext",
                role = Role.USER
            )
        )

        val prefs = userPreferenceRepository.findByUserId(created.id!!)
        assertNotNull(prefs)
        assertEquals(30, prefs.defaultLoanDurationDays)
        assertEquals("DD MMM YYYY", prefs.dateFormat)
        assertEquals("en", prefs.language)
        assertNull(prefs.libraryName)
    }
}
