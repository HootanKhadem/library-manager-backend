package com.dw.db

import com.dw.model.dto.Author

interface AuthorRepository {
    suspend fun save(author: Author): Author
    suspend fun searchByName(name: String): List<Author>
    suspend fun findByName(name: String): Author?
    suspend fun findById(id: Long): Author?
    suspend fun findAllByUserIdPaged(userId: Long, page: Int, pageSize: Int): List<Author>
    suspend fun countByUserId(userId: Long): Long
}
