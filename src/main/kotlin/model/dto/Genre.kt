package com.dw.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class Genre(
    val id: Long? = null,
    val name: String,
    val userId: Long? = null,
    val createdOn: String? = null,
    val createdBy: Long? = null,
    val modifiedOn: String? = null,
    val modifiedBy: Long? = null
)
