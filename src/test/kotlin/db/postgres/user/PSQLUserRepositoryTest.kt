package db.postgres.user

import com.dw.db.postgres.user.PSQLUserRepository
import com.dw.model.dto.Role
import com.dw.plugins.configureDatabases
import com.dw.service.util.PasswordUtil
import io.ktor.server.config.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*

class PSQLUserRepositoryTest {

    private val testConfig = MapApplicationConfig().apply {
        put("ktor.admin.username", "admin")
        put("ktor.admin.password", "admin")
        put("ktor.admin.email", "admin@example.com")
        put("ktor.psql-database.url", "jdbc:h2:mem:repo_test;DB_CLOSE_DELAY=-1")
        put("ktor.psql-database.username", "sa")
        put("ktor.psql-database.password", "")
        put("ktor.psql-database.driver", "org.h2.Driver")
    }

    @BeforeTest
    fun setup() {
        configureDatabases(testConfig)
    }

    @AfterTest
    fun tearDown() {
        transaction {
            SchemaUtils.drop(com.dw.db.mapping.UserTable)
        }
    }

    @Test
    fun `test createAdminUser creates user when not exists`() = runBlocking {
        val repo = PSQLUserRepository(testConfig)
        
        repo.createAdminUser()
        
        val user = repo.findByUsername("admin")
        assertNotNull(user)
        assertEquals("admin", user.name)
        assertEquals(Role.ADMIN, user.role)
        assertEquals("admin@example.com", user.email)
        assertTrue(PasswordUtil.verify("admin", user.salt ?: "", user.password))
    }

    @Test
    fun `test createAdminUser does not duplicate user`() = runBlocking {
        val repo = PSQLUserRepository(testConfig)
        
        repo.createAdminUser()
        val firstUser = repo.findByUsername("admin")
        assertNotNull(firstUser)
        
        repo.createAdminUser() // Call again
        val secondUser = repo.findByUsername("admin")
        assertNotNull(secondUser)
        
        assertEquals(firstUser.id, secondUser.id)
    }

    @Test
    fun `test createAdminUser with default values when config is missing`() = runBlocking {
        val repo = PSQLUserRepository(null)
        
        repo.createAdminUser()
        
        val user = repo.findByUsername("admin")
        assertNotNull(user)
        assertEquals("admin", user.name)
    }
}
