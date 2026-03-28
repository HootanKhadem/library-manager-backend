package com.dw.db

import com.dw.model.User

interface UserRepository {
    fun findByUsername(username: String): User?
    fun save(user: User): User
    fun update(user: User): User
}