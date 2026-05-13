package com.dw.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class Member(
    val id: Long = 0,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val userId: Long? = null,
    val createdOn: String? = null,
    val createdBy: Long? = null,
    val modifiedOn: String? = null,
    val modifiedBy: Long? = null
)
