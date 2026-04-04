package service.authentication

import TestDatabase
import com.dw.UserNotFoundException
import com.dw.db.postgres.user.PSQLUserRepository
import com.dw.model.dto.Role
import com.dw.model.dto.UserDTO
import com.dw.service.authentication.LoginServiceImpl
import com.dw.service.util.PasswordUtil
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import kotlin.test.*

/**
 * LoginServiceImplTest has been updated to use DatabaseContainer for common test setup.
 * This simplifies the test class and ensures consistent database state across the project.
 */
class LoginServiceImplTest {

    private val loginService = LoginServiceImpl()
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
    fun validLogin() = runBlocking {
        // The user requested a test for valid login.
        // Currently, LoginServiceImpl.login is a stub that returns "token".
        // This test ensures that the service returns the expected token for a valid-looking request.

        val username = "testuser"
        val password = "password"
        val salt = PasswordUtil.generateSalt()
        userRepository.save(
            UserDTO(
                name = username,
                email = "test@example.com",
                password = PasswordUtil.hashWithSalt(password, salt),
                salt = salt,
                role = Role.USER
            )
        )

        val token = loginService.login(username, password)

        assertNotNull(token, "Login should return a token")
        assertEquals("token", token, "For now, the stub should return 'token'")
    }

    @Test
    fun `non-existing user login should fail test`(): Unit = runBlocking {
        val username = "non-existing-user"
        val password = "password"
        assertThrows(UserNotFoundException::class.java) { 
            runBlocking { loginService.login(username, password) } 
        }
    }

    @Test
    fun `valid login with incorrect credentials`(): Unit = runBlocking {
        val username = "testuser"
        val password = "wrongpassword"
        val salt = PasswordUtil.generateSalt()
        userRepository.save(
            UserDTO(
                name = username,
                email = "test@example.com",
                password = PasswordUtil.hashWithSalt("password", salt),
                salt = salt,
                role = Role.USER
            )
        )
        assertThrows(UserNotFoundException::class.java) {
            runBlocking { loginService.login(username, password) }
        }
    }
}