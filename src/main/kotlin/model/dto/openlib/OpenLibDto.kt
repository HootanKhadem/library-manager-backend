package com.dw.model.dto.openlib

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class OpenLibSearchResponse(
    val numFound: Int = 0,
    val docs: List<OpenLibSearchDoc> = emptyList()
)

@Serializable
data class OpenLibSearchDoc(
    val title: String? = null,
    @SerializedName("author_name")
    val authorName: List<String> = emptyList(),
    @SerializedName("first_publish_year")
    val firstPublishYear: Int? = null,
    val publisher: List<String> = emptyList(),
    @SerializedName("number_of_pages_median")
    val numberOfPagesMedian: Int? = null
)

@Serializable
data class BookLookupDto(
    val title: String? = null,
    val author: String? = null,
    val publisher: String? = null,
    val publishedDate: String? = null,
    val pages: Int? = null
)
