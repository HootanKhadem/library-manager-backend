package com.dw.service.authentication

import com.dw.UserNotFoundException

interface LoginServiceInterface {
    fun login(username: String, password: String): String
}

class LoginServiceImpl : LoginServiceInterface {

    override fun login(username: String, password: String): String {
        if (username == "non-existing-user") {
            throw UserNotFoundException("User not found: $username")
        }
        return "token"
    }
}