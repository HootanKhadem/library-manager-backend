package com.dw.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class Book(
    private val name: String,
    private val author: Author,
    private val pages: Int,
    private val isbn: String,
    private val publishedDate: String,
    private val publisher: String,
    private val quantity: Int,
    private val image: String?
) {
}