package com.dw.service.admin

import com.dw.db.UserRepository
import com.dw.model.dto.UserDTO
import com.dw.service.util.PasswordUtil
import java.time.LocalDateTime

interface CreateUserServiceInterface {
    suspend fun createNewUser(userDTO: UserDTO, createdBy: Long? = null): UserDTO
}


class CreateUserService(private val userRepository: UserRepository) : CreateUserServiceInterface {

    override suspend fun createNewUser(userDTO: UserDTO, createdBy: Long?): UserDTO {
        val salt = PasswordUtil.generateSalt()
        val hashedPassword = PasswordUtil.hashWithSalt(userDTO.password, salt)
        val now = LocalDateTime.now().toString()
        val userToSave = userDTO.copy(
            password = hashedPassword,
            salt = salt,
            createdOn = now,
            createdBy = createdBy,
            modifiedOn = now,
            modifiedBy = createdBy
        )
        return userRepository.save(userToSave)
    }
}
