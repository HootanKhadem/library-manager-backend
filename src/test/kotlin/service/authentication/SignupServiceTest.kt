package service.authentication

import com.dw.EmailAlreadyExistsException
import com.dw.db.GenreRepository
import com.dw.db.UserRepository
import com.dw.db.postgres.genre.PSQLGenreRepository
import com.dw.db.postgres.preference.PSQLUserPreferenceRepository
import com.dw.db.postgres.user.PSQLUserRepository
import com.dw.model.dto.Role
import com.dw.model.dto.SignupRequest
import com.dw.model.dto.UserDTO
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

    @Test
    fun `signup converts a save-time unique constraint violation into EmailAlreadyExistsException`(): Unit = runBlocking {
        // Simulates the TOCTOU race described in the finding: two concurrent signups can both
        // pass the findByEmail check (each check and each save runs in its own independently-
        // committed transaction), so the second save() is the one that actually hits the DB's
        // UNIQUE constraint on email. We reproduce that end state directly - no real concurrency
        // needed - by stubbing findByEmail to always report "no such user" while save() still
        // writes to the real table, so the second signup's save() collides for real.
        val raceyUserRepository = object : UserRepository by userRepository {
            override suspend fun findByEmail(email: String): UserDTO? = null
        }
        val raceyService = SignupService(
            userRepository = raceyUserRepository,
            genreRepository = genreRepository,
            userPreferenceRepository = userPreferenceRepository,
            userPreferenceService = userPreferenceService,
            jwtService = jwtService
        )

        raceyService.signup(SignupRequest(name = "racer1", email = "race@example.com", password = "Password1"))

        assertFailsWith<EmailAlreadyExistsException> {
            raceyService.signup(SignupRequest(name = "racer2", email = "race@example.com", password = "Password1"))
        }
    }

    @Test
    fun `userRepository delete removes the user row`(): Unit = runBlocking {
        val user = userRepository.save(
            UserDTO(
                name = "deleteme",
                email = "deleteme@example.com",
                password = "hashed",
                role = Role.USER,
                salt = "salt",
                createdOn = null,
                createdBy = null,
                modifiedOn = null,
                modifiedBy = null
            )
        )

        val deleted = userRepository.delete(user.id!!)

        assertTrue(deleted)
        assertNull(userRepository.findByEmail("deleteme@example.com"))
    }

    @Test
    fun `signup deletes the just-created user if genre seeding fails afterward`(): Unit = runBlocking {
        // Forces the seeding-failure path directly (GenreRepository is an interface, so we can
        // stub it to throw) rather than trying to trigger a real infrastructure failure. This
        // exercises the exact try/catch + compensating-delete logic added to SignupService: the
        // user row must not survive a failed signup, and the original exception must propagate.
        val failingGenreRepository = object : GenreRepository by genreRepository {
            override suspend fun seedDefaults(userId: Long) {
                throw RuntimeException("genre seeding boom")
            }
        }
        val serviceWithFailingGenreSeed = SignupService(
            userRepository = userRepository,
            genreRepository = failingGenreRepository,
            userPreferenceRepository = userPreferenceRepository,
            userPreferenceService = userPreferenceService,
            jwtService = jwtService
        )

        assertFailsWith<RuntimeException> {
            serviceWithFailingGenreSeed.signup(
                SignupRequest(name = "ghost", email = "ghost@example.com", password = "Password1")
            )
        }

        assertNull(userRepository.findByEmail("ghost@example.com"))
    }
}
