package com.dw.model

import kotlinx.serialization.Serializable

@Serializable
data class Author(
    private val name: String,
    private val image: String
) {
}