package com.dw.service.admin

import com.dw.db.UserRepository
import com.dw.model.dto.UserDTO
import com.dw.service.util.PasswordUtil

interface CreateUserServiceInterface {
    suspend fun createNewUser(userDTO: UserDTO): UserDTO
}


class CreateUserService(private val userRepository: UserRepository) : CreateUserServiceInterface{

    override suspend fun createNewUser(userDTO: UserDTO): UserDTO {
        val salt = PasswordUtil.generateSalt()
        val hashedPassword = PasswordUtil.hashWithSalt(userDTO.password, salt)
        val userToSave = userDTO.copy(password = hashedPassword, salt = salt)
        return userRepository.save(userToSave)
    }
}