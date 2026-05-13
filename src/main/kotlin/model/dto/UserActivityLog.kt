package com.dw.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserActivityLog(
    val id: Long,
    val userId: Long,
    val userName: String,
    val action: String,
    val createdOn: String? = null,
    val createdBy: Long? = null,
    val modifiedOn: String? = null,
    val modifiedBy: Long? = null
)