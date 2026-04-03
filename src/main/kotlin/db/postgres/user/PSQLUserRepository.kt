package com.dw.db.postgres.user

import com.dw.db.UserRepository
import com.dw.db.mapping.UserDAO
import com.dw.db.mapping.UserTable
import com.dw.db.withTransaction
import com.dw.model.dto.UserDTO
import org.jetbrains.exposed.v1.core.eq

class PSQLUserRepository : UserRepository {
    override suspend fun findByUsername(username: String): UserDTO? =
        withTransaction { UserDAO.find { UserTable.username eq username }.firstOrNull()?.toUserDto() }


    override suspend fun save(userDTO: UserDTO): UserDTO = withTransaction {
        UserDAO.new {
            updateFromDto(userDTO)
            salt = "" // Stub for now
        }.toUserDto()
    }

    override suspend fun update(userDTO: UserDTO): UserDTO = withTransaction {
        val user = UserDAO.findById(userDTO.id ?: throw IllegalArgumentException("User ID is required for update"))
            ?: throw NoSuchElementException("User not found with ID ${userDTO.id}")
        user.updateFromDto(userDTO)
        user.toUserDto()
    }
}