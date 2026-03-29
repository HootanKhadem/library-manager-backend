package com.dw.db

import com.dw.model.dto.UserDTO

interface UserRepository {
    suspend fun findByUsername(username: String): UserDTO?
    suspend fun save(userDTO: UserDTO): UserDTO
    suspend fun update(userDTO: UserDTO): UserDTO
}