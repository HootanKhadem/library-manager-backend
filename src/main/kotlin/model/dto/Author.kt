package com.dw.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class Author(
    private val name: String,
    private val image: String
) {
}