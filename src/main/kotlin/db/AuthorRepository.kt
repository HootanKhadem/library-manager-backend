package com.dw.db

import com.dw.model.dto.Author

interface AuthorRepository {
    suspend fun save(author: Author): Author
    suspend fun searchByName(userId: Long, name: String, limit: Int = 10): List<Author>
    suspend fun findByName(userId: Long, name: String): Author?
    suspend fun findById(id: Long): Author?
    suspend fun findAllByUserIdPaged(userId: Long, page: Int, pageSize: Int, sort: String? = null, desc: Boolean = false): List<Author>
    suspend fun countBooks(authorId: Long): Long
    suspend fun countByUserId(userId: Long): Long

    /** (authorId, title) for every book of the given authors. */
    suspend fun findBookTitles(authorIds: List<Long>): List<Pair<Long, String>>

    /** Atomically moves every book of [sourceIds] to [targetId], deletes the sources and optionally renames the target. */
    suspend fun merge(targetId: Long, sourceIds: List<Long>, name: String?, modifiedBy: Long, modifiedOn: String): Author
}
