package com.dw.service.authentication

import com.dw.UserNotFoundException
import com.dw.db.UserRepository
import com.dw.service.util.PasswordUtil

interface LoginServiceInterface {
    suspend fun login(username: String, password: String): String
}

class LoginServiceImpl(
    private val userRepository: UserRepository,
    private val jwtService: JwtService
) : LoginServiceInterface {

    override suspend fun login(username: String, password: String): String {
        val user = userRepository.findByUsername(username)
            ?: throw UserNotFoundException("User not found: $username")

        if (!PasswordUtil.verify(password, user.salt ?: "", user.password)) {
            throw UserNotFoundException("Invalid credentials") // Reusing for now as per the test's expectation
        }

        return jwtService.generateToken(user)
    }
}