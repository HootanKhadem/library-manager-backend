package com.dw.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class Author(
    val id: Long? = null,
    val name: String,
    val image: String,
    val userId: Long? = null,
    val bookCount: Long? = null,
    val createdOn: String? = null,
    val createdBy: Long? = null,
    val modifiedOn: String? = null,
    val modifiedBy: Long? = null
)

/** What a merge would do: total books afterwards and titles that appear under more than one of the merged authors. */
@Serializable
data class MergePreview(
    val bookCount: Long,
    val duplicateTitles: List<String>
)

/** Merge [sourceIds] into [targetId]; [name] optionally renames the surviving author. */
@Serializable
data class MergeAuthorsRequest(
    val targetId: Long,
    val sourceIds: List<Long>,
    val name: String? = null
)
