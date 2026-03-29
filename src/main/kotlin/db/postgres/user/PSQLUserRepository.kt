package com.dw.db.postgres.user

import com.dw.db.UserRepository
import com.dw.model.dto.UserDTO

class PSQLUserRepository : UserRepository {
    override fun findByUsername(username: String): UserDTO? {
        TODO("Not yet implemented")
    }

    override fun save(userDTO: UserDTO): UserDTO {
        TODO("Not yet implemented")
    }

    override fun update(userDTO: UserDTO): UserDTO {
        TODO("Not yet implemented")
    }
}