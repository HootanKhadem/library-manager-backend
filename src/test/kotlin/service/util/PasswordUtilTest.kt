package service.util

import com.dw.service.util.PasswordUtil
import com.dw.service.util.PasswordUtil.Companion.md5
import com.dw.service.util.PasswordUtil.Companion.sha256
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordUtilTest {

    private val passwordUtil = PasswordUtil()

    @Test
    fun `test md5 hash for empty string`() {
        val input = ""
        val expected = "d41d8cd98f00b204e9800998ecf8427e"
        assertEquals(expected, input.md5())

    }

    @Test
    fun `test md5 hash for common password`() {
        val input = "123456"
        val expected = "e10adc3949ba59abbe56e057f20f883e"
        assertEquals(expected, input.md5())
    }

    @Test
    fun `test sha256 hash for empty string`() {
        val input = ""
        val expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        assertEquals(expected, input.sha256())
    }

    @Test
    fun `test sha256 hash for common password`() {
        val input = "password"
        val expected = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"
        assertEquals(expected, input.sha256())
    }

    @Test
    fun `test verify returns true for matching password and salt`() {
        val password = "mySecretPassword"
        val salt = PasswordUtil.generateSalt()
        val hashedPassword = PasswordUtil.hashWithSalt(password, salt)
        assertTrue(PasswordUtil.verify(password, salt, hashedPassword))
    }

    @Test
    fun `test verify returns false for non-matching password`() {
        val password = "mySecretPassword"
        val salt = PasswordUtil.generateSalt()
        val hashedPassword = PasswordUtil.hashWithSalt(password, salt)
        assertFalse(PasswordUtil.verify("wrongPassword", salt, hashedPassword))
    }

    @Test
    fun `test generateSalt returns unique values`() {
        val salt1 = PasswordUtil.generateSalt()
        val salt2 = PasswordUtil.generateSalt()
        assertFalse(salt1 == salt2)
    }

    @Test
    fun `test hashWithSalt is consistent`() {
        val password = "password"
        val salt = "constantSalt"
        assertEquals(PasswordUtil.hashWithSalt(password, salt), PasswordUtil.hashWithSalt(password, salt))
    }

    @Test
    fun `test md5 hash consistency`() {
        val input = "consistentInput"
        assertEquals(input.md5(), input.md5())
    }

    @Test
    fun `test sha256 hash consistency`() {
        val input = "consistentInput"
        assertEquals(input.sha256(), input.sha256())
    }

    @Test
    fun `test md5 with special characters`() {
        val input = "p@ssw0rd! £$€ 😊"
        // echo -n "p@ssw0rd! £$€ 😊" | md5sum
        // Note: the result depends on encoding. UTF-8 is assumed.
        val expected = "2acf4cbd9d6ebabbccf6ba7a708e4dda"
        assertEquals(expected, input.md5())
    }

    @Test
    fun `test sha256 with special characters`() {
        val input = "p@ssw0rd! £$€ 😊"
        // echo -n "p@ssw0rd! £$€ 😊" | sha256sum
        val expected = "01c274f40993c10d0d320da481e9df49a188ff8724699e62542609ebcf8bb00f"
        assertEquals(expected, input.sha256())
    }
}
