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
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
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

        request.preferences?.let { userPreferenceService.validatePreferences(it) }

        val salt = PasswordUtil.generateSalt()
        val hashedPassword = PasswordUtil.hashWithSalt(request.password, salt)
        val now = LocalDateTime.now().toString()

        // The findByEmail check above and the save() below each run in their own independently-
        // committed transaction (see db/withTransaction.kt: inTopLevelSuspendTransaction always
        // starts a fresh transaction, nothing composes them into one atomic unit). Two concurrent
        // signups for the same email can therefore both pass the check above and race to save().
        // The DB's UNIQUE constraint on user.email (see V1__initial_schema.sql) is the real
        // safeguard in that case; translate its low-level SQL exception into the documented 409.
        val savedUser = try {
            userRepository.save(
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
        } catch (e: ExposedSQLException) {
            throw EmailAlreadyExistsException("Email already registered: ${request.email}")
        }

        // Genre/preference seeding below runs in transactions separate from the user save above
        // (same reason as the comment above this block), so there is no shared transaction to
        // roll back if seeding fails. This is a compensating action, not a real rollback: delete
        // the just-created user row so a failed signup doesn't leave a "ghost" account behind
        // (a claimed email with no genre/preference rows), then re-throw the original exception
        // so the caller still sees the real failure.
        try {
            genreRepository.seedDefaults(savedUser.id!!)

            if (request.preferences != null) {
                userPreferenceService.savePreferences(savedUser.id, request.preferences)
            } else {
                userPreferenceRepository.seedDefaults(savedUser.id)
            }
        } catch (e: Exception) {
            userRepository.delete(savedUser.id!!)
            throw e
        }

        val tokens = jwtService.generateToken(savedUser)
        return savedUser to tokens
    }
}
