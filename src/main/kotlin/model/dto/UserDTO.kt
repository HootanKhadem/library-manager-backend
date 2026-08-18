package com.dw.model.dto

import kotlinx.serialization.*


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
    val salt: String? = null,
    val createdOn: String? = null,
    val createdBy: Long? = null,
    val modifiedOn: String? = null,
    val modifiedBy: Long? = null
)

@Serializable
data class LoginDTO(val email: String, val password: String)

@Serializable
data class SignupRequest(
    val name: String,
    val email: String,
    val password: String,
    val preferences: UserPreference? = null
)
