package com.dw.service.util

import java.security.MessageDigest

class PasswordUtil {

    fun String.md5(): String {
        return hashString(this, "MD5")
    }

    fun String.sha256(): String {
        return hashString(this, "SHA-256")
    }

    fun verify(password: String, hashedPassword: String): Boolean {
        return password == hashedPassword
    }


    private fun hashString(input: String, algorithm: String): String {
        return MessageDigest
            .getInstance(algorithm)
            .digest(input.toByteArray())
            .fold("", { str, it -> str + "%02x".format(it) })
    }
}