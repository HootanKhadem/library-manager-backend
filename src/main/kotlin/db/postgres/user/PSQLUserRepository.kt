package com.dw.db.postgres.user

import com.dw.db.UserRepository
import com.dw.db.mapping.UserDAO
import com.dw.db.mapping.UserTable
import com.dw.db.withTransaction
import com.dw.model.dto.Role
import com.dw.model.dto.UserDTO
import com.dw.service.util.PasswordUtil
import io.ktor.server.config.*
import org.jetbrains.exposed.v1.core.eq

class PSQLUserRepository(private val config: ApplicationConfig? = null) : UserRepository {
    override suspend fun findByUsername(username: String): UserDTO? =
        withTransaction {
            UserDAO.find { UserTable.username eq username }
                .firstOrNull()
                ?.toUserDto()
        }


    override suspend fun save(userDTO: UserDTO): UserDTO = withTransaction {
        UserDAO.new {
            updateFromDto(userDTO)
        }.toUserDto()
    }

    override suspend fun update(userDTO: UserDTO): UserDTO = withTransaction {
        val user = UserDAO.findById(userDTO.id ?: throw IllegalArgumentException("User ID is required for update"))
            ?: throw NoSuchElementException("User not found with ID ${userDTO.id}")
        user.updateFromDto(userDTO)
        user.toUserDto()
    }

    override suspend fun createAdminUser() {
        val adminUsername = config?.propertyOrNull("ktor.admin.username")?.getString() ?: "admin"
        val adminPassword = config?.propertyOrNull("ktor.admin.password")?.getString() ?: "admin"
        val adminEmail = config?.propertyOrNull("ktor.admin.email")?.getString() ?: "admin@example.com"

        withTransaction {
            val exists = UserDAO.find { UserTable.username eq adminUsername }.count() > 0
            if (!exists) {
                val salt = PasswordUtil.generateSalt()
                val hashedPassword = PasswordUtil.hashWithSalt(adminPassword, salt)
                UserDAO.new {
                    username = adminUsername
                    password = hashedPassword
                    email = adminEmail
                    role = Role.ADMIN.name
                    this.salt = salt
                }
            }
        }
    }
}