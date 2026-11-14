package com.dw.db

import com.dw.model.dto.Book

interface BookRepository {
    suspend fun save(book: Book): Book
    suspend fun findById(id: Long): Book?
    suspend fun findAllByUserId(userId: Long): List<Book>
    suspend fun findAllByUserIdPaged(userId: Long, page: Int, pageSize: Int, search: String? = null, status: String? = null): List<Book>
    suspend fun findAllByAuthorId(userId: Long, authorId: Long): List<Book>
    suspend fun update(id: Long, book: Book): Book?
    suspend fun delete(id: Long): Boolean
    suspend fun countByUserId(userId: Long, search: String? = null, status: String? = null): Long
    suspend fun findRecentlyAdded(userId: Long, limit: Int): List<Book>
    suspend fun countAddedThisMonth(userId: Long, yearMonth: String): Long
}
