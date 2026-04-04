package db.mapping

import TestDatabase
import com.dw.db.postgres.user.PSQLUserRepository
import com.dw.model.dto.Role
import com.dw.model.dto.UserDTO
import kotlinx.coroutines.runBlocking
import kotlin.test.*

/**
 * UserDAOTest has been updated to use H2 in-memory database.
 * This ensures that we are testing the actual database interaction logic
 * rather than a mock repository.
 * 
 * H2 configuration details:
 * - Driver: org.h2.Driver
 * - URL: jdbc:h2:mem:test_user_dao;DB_CLOSE_DELAY=-1
 *   - 'mem:test_user_dao' creates an in-memory database named 'test_user_dao'.
 *   - 'DB_CLOSE_DELAY=-1' ensures the database is not dropped when the last connection closes,
 *     which is useful when using connection pooling or multiple transactions in a test session.
 */
class UserDAOTest {

    private val userRepository = PSQLUserRepository()
    @BeforeTest
    fun setup() {
        TestDatabase.init()
    }

    @AfterTest
    fun tearDown() {
        TestDatabase.tearDown()
    }

    @Test
    fun testMockUserRepo() = runBlocking {
        val username = "testuser"
        userRepository.save(UserDTO(id = null, name = username, email = "test@mail.com", password = "password", role = Role.USER))

        val user = userRepository.findByUsername(username)

        assertNotNull(user)
        assertEquals(username, user.name)
        assertEquals(Role.USER, user.role)
    }

    @Test
    fun testMockUserRepoNoUser() = runBlocking {
        val user = userRepository.findByUsername("no-user")
        assertEquals(null, user)
    }

    @Test
    fun testMockUserRepoSave() = runBlocking {
        val dto = UserDTO(id = null, name = "newuser", email = "new@mail.com", password = "pass", role = Role.ADMIN)
        val saved = userRepository.save(dto)

        assertNotNull(saved.id)
        assertEquals(dto.name, saved.name)
        assertEquals(dto.role, saved.role)
    }

    @Test
    fun testMockUserRepoUpdate() = runBlocking {
        val saved = userRepository.save(UserDTO(id = null, name = "u", email = "m", password = "p", role = Role.USER))
        val dto = UserDTO(id = saved.id, name = "updated", email = "u@mail.com", password = "pass", role = Role.ADMIN, salt = "")
        val updated = userRepository.update(dto)

        assertEquals(dto, updated)
    }
}
