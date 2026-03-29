package com.dw.db

import com.dw.model.dto.UserDTO

interface UserRepository {
    fun findByUsername(username: String): UserDTO?
    fun save(userDTO: UserDTO): UserDTO
    fun update(userDTO: UserDTO): UserDTO
}