package com.dw.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class Lending(
    val id: Long? = null,
    val bookId: Long,
    val memberId: Long,
    val userId: Long? = null,
    val lentDate: String,
    val expectedReturnDate: String? = null,
    val actualReturnDate: String? = null,
    val status: String = LendingStatus.ACTIVE.name,
    val createdOn: String? = null,
    val createdBy: Long? = null,
    val modifiedOn: String? = null,
    val modifiedBy: Long? = null
)
