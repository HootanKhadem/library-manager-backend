package com.dw.service.authentication

interface LoginServiceInterface {
    fun login(username: String, password: String): String
}

class LoginServiceImpl : LoginServiceInterface {

    private val name = "userLoginService"
    override fun login(username: String, password: String): String {
        return "token"
    }
}