package com.dw.model.dto.dashboard

import kotlinx.serialization.Serializable

@Serializable
data class BookStats(
    val totalBooks: Long,
    val addedThisMonth: Long
)

@Serializable
data class LentOutStats(
    val totalLentOut: Long,
    val uniqueLendees: Long
)

@Serializable
data class OverdueStats(
    val totalOverdue: Long
)

@Serializable
data class RecentlyAddedBook(
    val id: Long,
    val name: String,
    val author: String,
    val genre: String?,
    val status: String?,
    val rating: Int?
)

@Serializable
data class ActivityLogEntry(
    val id: Long,
    val action: String,
    val bookName: String?,
    val memberName: String?,
    val occurredAt: String?
)
