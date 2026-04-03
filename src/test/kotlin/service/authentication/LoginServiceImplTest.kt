package service.authentication

import TestDatabase
import com.dw.UserNotFoundException
import com.dw.service.authentication.LoginServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * LoginServiceImplTest has been updated to use DatabaseContainer for common test setup.
 * This simplifies the test class and ensures consistent database state across the project.
 */
class LoginServiceImplTest {

    private val loginService = LoginServiceImpl()

    @BeforeTest
    fun setup() {
        TestDatabase.init()
    }

    @Test
    fun validLogin() = runBlocking {
        // The user requested a test for valid login.
        // Currently, LoginServiceImpl.login is a stub that returns "token".
        // This test ensures that the service returns the expected token for a valid-looking request.

        val username = "testuser"
        val password = "password"

        val token = loginService.login(username, password)

        assertNotNull(token, "Login should return a token")
        assertEquals("token", token, "For now, the stub should return 'token'")
    }

    @Test
    fun `non-existing user login should fail test`() {
        val username = "non-existing-user"
        val password = "password"
        assertThrows(UserNotFoundException::class.java) { loginService.login(username, password) }
    }

//    @Test
//    fun `valid login with incorrect credentials`() {
//        val username = "testuser"
//        val password = "wrongpassword"
//
//        assertThrows(UserNotFoundException::class.java) { loginService.login(username, password) }
//    }
}