package com.dw.service.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

class PasswordUtil {

    companion object {
        fun generateSalt(): String {
            val random = SecureRandom()
            val bytes = ByteArray(16)
            random.nextBytes(bytes)
            return Base64.getEncoder().encodeToString(bytes)
        }

        fun hashWithSalt(password: String, salt: String): String {
            return String.format("%s%s", password, salt).sha256()
        }

        fun String.md5(): String {
            return hashString(this, "MD5")
        }

        fun String.sha256(): String {
            return hashString(this, "SHA-256")
        }

        fun verify(password: String, salt: String, hashedPassword: String): Boolean {
            return hashWithSalt(password, salt) == hashedPassword
        }


        private fun hashString(input: String, algorithm: String): String {
            return MessageDigest
                .getInstance(algorithm)
                .digest(input.toByteArray(Charsets.UTF_8))
                .fold("", { str, it -> str + "%02x".format(it) })
        }
    }
}