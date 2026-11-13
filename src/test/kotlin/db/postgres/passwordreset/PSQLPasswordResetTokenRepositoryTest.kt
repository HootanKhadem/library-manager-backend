package db.postgres.passwordreset

import com.dw.db.mapping.UserDAO
import com.dw.db.postgres.passwordreset.PSQLPasswordResetTokenRepository
import com.dw.model.dto.Role
import com.dw.plugins.configureDatabases
import io.ktor.server.config.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime
import kotlin.test.*

class PSQLPasswordResetTokenRepositoryTest {

    private val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:repo_reset_token_test;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver"
    )

    private val repository = PSQLPasswordResetTokenRepository()

    @BeforeTest
    fun setup() = configureDatabases(testConfig)

    @AfterTest
    fun tearDown() {
        transaction { exec("DROP ALL OBJECTS") }
    }

    private fun createUser(n: Int): Long = transaction {
        UserDAO.new {
            username = "user$n"
            password = "p"
            email = "user$n@example.com"
            role = Role.USER.name
            salt = "s"
        }.id.value
    }

    @Test
    fun `save then findByHash returns userId and expiry`(): Unit = runBlocking {
        val id = createUser(1)
        val expiry = LocalDateTime.of(2030, 1, 1, 12, 0)
        repository.save(id, "hash1", expiry)

        val found = repository.findByHash("hash1")
        assertNotNull(found)
        assertEquals(id, found.userId)
        assertEquals(expiry, found.expiresOn)
    }

    @Test
    fun `findByHash returns null for unknown hash`(): Unit = runBlocking {
        assertNull(repository.findByHash("nope"))
    }

    @Test
    fun `deleteByUserId removes only that user's tokens`(): Unit = runBlocking {
        val a = createUser(1)
        val b = createUser(2)
        val expiry = LocalDateTime.now().plusHours(1)
        repository.save(a, "ha1", expiry)
        repository.save(a, "ha2", expiry)
        repository.save(b, "hb", expiry)

        repository.deleteByUserId(a)

        assertNull(repository.findByHash("ha1"))
        assertNull(repository.findByHash("ha2"))
        assertNotNull(repository.findByHash("hb"))
    }
}
