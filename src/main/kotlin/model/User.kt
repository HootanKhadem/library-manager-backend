package com.dw.model

data class User(
    private val name: String,
    private val email: String,
    private val password: String,
    private val role: Role
) {
}