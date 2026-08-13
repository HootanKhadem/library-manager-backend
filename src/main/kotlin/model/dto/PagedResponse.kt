package com.dw.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class PagedResponse<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalItems: Long,
    val totalPages: Int
) {
    companion object {
        fun <T> of(items: List<T>, page: Int, pageSize: Int, totalItems: Long): PagedResponse<T> {
            val totalPages = maxOf(1, ((totalItems + pageSize - 1) / pageSize).toInt())
            return PagedResponse(items, page, pageSize, totalItems, totalPages)
        }
    }
}
