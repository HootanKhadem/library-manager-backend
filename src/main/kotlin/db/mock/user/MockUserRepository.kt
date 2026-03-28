package com.dw.db.mock.user

import com.dw.db.UserRepository
import com.dw.model.Role
import com.dw.model.User
import kotlin.random.Random

class MockUserRepository : UserRepository {
    override fun findByUsername(username: String): User?  {
        if (username == "no-user") {
            return null
        }
        return User(Random.nextLong() ,username, "user@mail.com", "password", Role.USER)
    }

    override fun save(user: User): User {
        return user.copy(id = Random.nextLong())
    }

    override fun update(user: User): User {
        return user
    }
}