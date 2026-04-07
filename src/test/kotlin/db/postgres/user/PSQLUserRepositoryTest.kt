package db.postgres.user

import TestDatabase
import com.dw.db.postgres.user.PSQLUserRepository
import com.dw.model.dto.Role
import com.dw.service.util.PasswordUtil
import io.ktor.server.config.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class PSQLUserRepositoryTest {

    @BeforeTest
    fun setup() {
        TestDatabase.init()
    }

    @AfterTest
    fun tearDown() {
        TestDatabase.tearDown()
    }

    @Test
    fun `test createAdminUser creates user when not exists`() = runBlocking {
        val config = MapApplicationConfig().apply {
            put("ktor.admin.username", "admin")
            put("ktor.admin.password", "admin")
            put("ktor.admin.email", "admin@example.com")
        }
        val repo = PSQLUserRepository(config)
        
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
        val config = MapApplicationConfig().apply {
            put("ktor.admin.username", "admin")
            put("ktor.admin.password", "admin")
        }
        val repo = PSQLUserRepository(config)
        
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
