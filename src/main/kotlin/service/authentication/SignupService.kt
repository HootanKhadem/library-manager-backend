package com.dw.service.authentication

import com.dw.EmailAlreadyExistsException
import com.dw.db.GenreRepository
import com.dw.db.UserPreferenceRepository
import com.dw.db.UserRepository
import com.dw.model.dto.Role
import com.dw.model.dto.SignupRequest
import com.dw.model.dto.UserDTO
import com.dw.service.preference.UserPreferenceServiceInterface
import com.dw.service.util.PasswordUtil
import java.time.LocalDateTime

interface SignupServiceInterface {
    suspend fun signup(request: SignupRequest): Pair<UserDTO, Pair<String, String>>
}

class SignupService(
    private val userRepository: UserRepository,
    private val genreRepository: GenreRepository,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val userPreferenceService: UserPreferenceServiceInterface,
    private val jwtService: JwtService
) : SignupServiceInterface {

    override suspend fun signup(request: SignupRequest): Pair<UserDTO, Pair<String, String>> {
        if (userRepository.findByEmail(request.email) != null) {
            throw EmailAlreadyExistsException("Email already registered: ${request.email}")
        }

        require(request.password.length >= 8) { "password must be at least 8 characters" }
        require(request.password.any { it.isUpperCase() }) { "password must contain at least one uppercase letter" }
        require(request.password.any { it.isDigit() }) { "password must contain at least one digit" }

        val salt = PasswordUtil.generateSalt()
        val hashedPassword = PasswordUtil.hashWithSalt(request.password, salt)
        val now = LocalDateTime.now().toString()

        val savedUser = userRepository.save(
            UserDTO(
                name = request.name,
                email = request.email,
                password = hashedPassword,
                role = Role.USER,
                salt = salt,
                createdOn = now,
                createdBy = null,
                modifiedOn = now,
                modifiedBy = null
            )
        )

        genreRepository.seedDefaults(savedUser.id!!)

        if (request.preferences != null) {
            userPreferenceService.savePreferences(savedUser.id, request.preferences)
        } else {
            userPreferenceRepository.seedDefaults(savedUser.id)
        }

        val tokens = jwtService.generateToken(savedUser)
        return savedUser to tokens
    }
}
