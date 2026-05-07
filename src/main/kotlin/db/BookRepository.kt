package com.dw.db

import com.dw.model.dto.Book

interface BookRepository {
    suspend fun save(book: Book): Book
    suspend fun findById(id: Long): Book?
    suspend fun findAllByUserId(userId: Long): List<Book>
    suspend fun update(id: Long, book: Book): Book?
    suspend fun delete(id: Long): Boolean
}
