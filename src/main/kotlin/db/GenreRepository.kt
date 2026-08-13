package com.dw.db

import com.dw.model.dto.Genre

interface GenreRepository {
    suspend fun save(genre: Genre): Genre
    suspend fun findByUserId(userId: Long): List<Genre>
    suspend fun findById(id: Long): Genre?
    suspend fun update(id: Long, genre: Genre): Genre?
    suspend fun delete(id: Long): Boolean
    suspend fun seedDefaults(userId: Long)
}
