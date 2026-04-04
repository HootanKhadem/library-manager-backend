package com.dw.service.util

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
        with(passwordUtil) {
            assertEquals(expected, input.md5())
        }
    }

    @Test
    fun `test md5 hash for common password`() {
        val input = "123456"
        val expected = "e10adc3949ba59abbe56e057f20f883e"
        with(passwordUtil) {
            assertEquals(expected, input.md5())
        }
    }

    @Test
    fun `test sha256 hash for empty string`() {
        val input = ""
        val expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        with(passwordUtil) {
            assertEquals(expected, input.sha256())
        }
    }

    @Test
    fun `test sha256 hash for common password`() {
        val input = "password"
        val expected = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"
        with(passwordUtil) {
            assertEquals(expected, input.sha256())
        }
    }

    @Test
    fun `test verify returns true for matching strings`() {
        val password = "mySecretPassword"
        val hashedPassword = "mySecretPassword"
        assertTrue(passwordUtil.verify(password, hashedPassword))
    }

    @Test
    fun `test verify returns false for non-matching strings`() {
        val password = "mySecretPassword"
        val hashedPassword = "wrongPassword"
        assertFalse(passwordUtil.verify(password, hashedPassword))
    }

    @Test
    fun `test md5 hash consistency`() {
        val input = "consistentInput"
        with(passwordUtil) {
            assertEquals(input.md5(), input.md5())
        }
    }

    @Test
    fun `test sha256 hash consistency`() {
        val input = "consistentInput"
        with(passwordUtil) {
            assertEquals(input.sha256(), input.sha256())
        }
    }

    @Test
    fun `test md5 with special characters`() {
        val input = "p@ssw0rd! £$€ 😊"
        // echo -n "p@ssw0rd! £$€ 😊" | md5sum
        // Note: the result depends on encoding. UTF-8 is assumed.
        val expected = "35f79366e4ca83c31406e6378e9f2911" 
        with(passwordUtil) {
            assertEquals(expected, input.md5())
        }
    }

    @Test
    fun `test sha256 with special characters`() {
        val input = "p@ssw0rd! £$€ 😊"
        // echo -n "p@ssw0rd! £$€ 😊" | sha256sum
        val expected = "3340570b575631526487e6717a0225642d2011116223592182607e05089e02a9"
        with(passwordUtil) {
            assertEquals(expected, input.sha256())
        }
    }
}
