package com.dw.model.dto

import kotlinx.serialization.Serializable


enum class Role {
    ADMIN,
    USER
}

@Serializable
data class UserDTO(
     val id: Long? = null,
     val name: String,
     val email: String,
     val password: String,
     val role: Role,
     val salt: String? = null
) {
}