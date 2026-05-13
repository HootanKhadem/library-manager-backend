package com.dw.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class Book(
    val id: Long? = null,
    val name: String,
    val author: Author,
    val translator: String? = null,
    val pages: Int,
    val isbn: String,
    val publishedDate: String,
    val publisher: String,
    val quantity: Int,
    val image: String? = null,
    val userId: Long? = null,
    val createdOn: String? = null,
    val createdBy: Long? = null,
    val modifiedOn: String? = null,
    val modifiedBy: Long? = null
)
