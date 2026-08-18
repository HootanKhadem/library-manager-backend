package service.authentication

import com.dw.EmailAlreadyExistsException
import com.dw.db.postgres.genre.PSQLGenreRepository
import com.dw.db.postgres.preference.PSQLUserPreferenceRepository
import com.dw.db.postgres.user.PSQLUserRepository
import com.dw.model.dto.SignupRequest
import com.dw.model.dto.UserPreference
import com.dw.plugins.JwtConfig
import com.dw.plugins.configureDatabases
import com.dw.service.authentication.JwtService
import com.dw.service.authentication.SignupService
import com.dw.service.preference.UserPreferenceServiceImpl
import io.ktor.server.config.MapApplicationConfig
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*

class SignupServiceTest {

    private val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:signup_service_test;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver"
    )

    private val userRepository = PSQLUserRepository()
    private val genreRepository = PSQLGenreRepository()
    private val userPreferenceRepository = PSQLUserPreferenceRepository()
    private val userPreferenceService = UserPreferenceServiceImpl(userPreferenceRepository)
    private val jwtService = JwtService(JwtConfig(secret = "secret", issuer = "issuer", audience = "audience", realm = "realm"))

    private val service = SignupService(
        userRepository = userRepository,
        genreRepository = genreRepository,
        userPreferenceRepository = userPreferenceRepository,
        userPreferenceService = userPreferenceService,
        jwtService = jwtService
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
    fun `signup creates a USER-role account, seeds genre and preference defaults, and returns tokens`(): Unit = runBlocking {
        val (created, tokens) = service.signup(
            SignupRequest(name = "newuser", email = "newuser@example.com", password = "Password1")
        )

        assertEquals("USER", created.role.name)
        assertNull(created.createdBy)
        assertNotNull(created.id)

        val prefs = userPreferenceRepository.findByUserId(created.id!!)
        assertNotNull(prefs)
        assertEquals(30, prefs.defaultLoanDurationDays)
        assertEquals("en", prefs.language)

        assertTrue(tokens.first.isNotBlank())
        assertTrue(tokens.second.isNotBlank())
        assertNotEquals(tokens.first, tokens.second)
    }

    @Test
    fun `signup saves provided preferences instead of defaults`(): Unit = runBlocking {
        val (created, _) = service.signup(
            SignupRequest(
                name = "prefuser",
                email = "prefuser@example.com",
                password = "Password1",
                preferences = UserPreference(libraryName = "My Library", language = "fa", dateFormat = "YYYY-MM-DD", defaultLoanDurationDays = 14)
            )
        )

        val prefs = userPreferenceRepository.findByUserId(created.id!!)
        assertNotNull(prefs)
        assertEquals("My Library", prefs.libraryName)
        assertEquals("fa", prefs.language)
        assertEquals("YYYY-MM-DD", prefs.dateFormat)
        assertEquals(14, prefs.defaultLoanDurationDays)
    }

    @Test
    fun `signup rejects a duplicate email without saving a new user`(): Unit = runBlocking {
        service.signup(SignupRequest(name = "first", email = "dup@example.com", password = "Password1"))

        assertFailsWith<EmailAlreadyExistsException> {
            service.signup(SignupRequest(name = "second", email = "dup@example.com", password = "Password1"))
        }
    }

    @Test
    fun `signup rejects a password shorter than 8 characters`(): Unit = runBlocking {
        assertFailsWith<IllegalArgumentException> {
            service.signup(SignupRequest(name = "u", email = "short@example.com", password = "Pass1"))
        }
    }

    @Test
    fun `signup rejects a password with no uppercase letter`(): Unit = runBlocking {
        assertFailsWith<IllegalArgumentException> {
            service.signup(SignupRequest(name = "u", email = "noupper@example.com", password = "password1"))
        }
    }

    @Test
    fun `signup rejects a password with no digit`(): Unit = runBlocking {
        assertFailsWith<IllegalArgumentException> {
            service.signup(SignupRequest(name = "u", email = "nodigit@example.com", password = "Password"))
        }
    }

    @Test
    fun `signup propagates invalid preference values`(): Unit = runBlocking {
        assertFailsWith<IllegalArgumentException> {
            service.signup(
                SignupRequest(
                    name = "u",
                    email = "badpref@example.com",
                    password = "Password1",
                    preferences = UserPreference(language = "xx")
                )
            )
        }
    }

    @Test
    fun `signup with invalid preferences does not create a user`(): Unit = runBlocking {
        assertFailsWith<IllegalArgumentException> {
            service.signup(
                SignupRequest(
                    name = "u",
                    email = "badpref@example.com",
                    password = "Password1",
                    preferences = UserPreference(language = "xx")
                )
            )
        }

        assertNull(userRepository.findByEmail("badpref@example.com"))
    }
}
