package com.dw.model

import kotlinx.serialization.Serializable


enum class Role {
    ADMIN,
    USER
}

@Serializable
data class User(
     val id: Long? = null,
     val name: String,
     val email: String,
     val password: String,
     val role: Role
) {
}