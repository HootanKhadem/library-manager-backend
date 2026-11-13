package com.dw.model.dto.openlib

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class OpenLibDto(
    val identifiers: Identifiers? = null,
    val title: String? = null,
    val authors: List<OpenLibReference> = emptyList(),
    @SerializedName("publish_date")
    val publishDate: String? = null,
    val publishers: List<String> = emptyList(),
    val covers: List<Int> = emptyList(),
    val contributions: List<String> = emptyList(),
    val languages: List<OpenLibReference> = emptyList(),
    @SerializedName("source_records")
    val sourceRecords: List<String> = emptyList(),
    @SerializedName("local_id")
    val localId: List<String> = emptyList(),
    val type: OpenLibReference? = null,
    @SerializedName("first_sentence")
    val firstSentence: OpenLibText? = null,
    val key: String? = null,
    @SerializedName("number_of_pages")
    val numberOfPages: Int? = null,
    val works: List<OpenLibReference> = emptyList(),
    val classifications: Map<String, List<String>> = emptyMap(),
    val ocaid: String? = null,
    @SerializedName("isbn_10")
    val isbn10: List<String> = emptyList(),
    @SerializedName("isbn_13")
    val isbn13: List<String> = emptyList(),
    @SerializedName("latest_revision")
    val latestRevision: Int? = null,
    val revision: Int? = null,
    val created: OpenLibText? = null,
    @SerializedName("last_modified")
    val lastModified: OpenLibText? = null
)

@Serializable
data class Identifiers(
    val goodreads: List<String> = emptyList(),
    val librarything: List<String> = emptyList()
)

@Serializable
data class OpenLibReference(
    val key: String? = null
)

@Serializable
data class OpenLibText(
    val type: String? = null,
    val value: String? = null
)

@Serializable
data class OpenLibAuthorDto(
    val name: String? = null
)

@Serializable
data class BookLookupDto(
    val title: String? = null,
    val author: String? = null,
    val publisher: String? = null,
    val publishedDate: String? = null,
    val pages: Int? = null
)
