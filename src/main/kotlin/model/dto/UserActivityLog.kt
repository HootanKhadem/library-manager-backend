package com.dw.model.dto

import kotlinx.serialization.Serializable

enum class ActivityAction { LENT, RETURNED, ADDED, REMOVED, UPDATED }

@Serializable
data class UserActivityLog(
    val id: Long? = null,
    val userId: Long,
    val userName: String? = null,
    val action: String,
    val bookId: Long? = null,
    val bookName: String? = null,
    val memberId: Long? = null,
    val memberName: String? = null,
    val createdOn: String? = null,
    val createdBy: Long? = null,
    val modifiedOn: String? = null,
    val modifiedBy: Long? = null
)
