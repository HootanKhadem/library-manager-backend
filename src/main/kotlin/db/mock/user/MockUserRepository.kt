package com.dw.db.mock.user

import com.dw.db.UserRepository
import com.dw.model.dto.Role
import com.dw.model.dto.UserDTO
import kotlin.random.Random

class MockUserRepository : UserRepository {
    override fun findByUsername(username: String): UserDTO?  {
        if (username == "no-user") {
            return null
        }
        return UserDTO(Random.nextLong() ,username, "user@mail.com", "password", Role.USER)
    }

    override fun save(userDTO: UserDTO): UserDTO {
        return userDTO.copy(id = Random.nextLong())
    }

    override fun update(userDTO: UserDTO): UserDTO {
        return userDTO
    }
}