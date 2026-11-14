package com.dw.service.author

import com.dw.db.AuthorRepository
import com.dw.model.dto.Author
import com.dw.model.dto.PagedResponse
import java.time.LocalDateTime


interface AuthorServiceInterface {
    suspend fun createAuthor(author: Author): Author
    suspend fun searchAuthors(userId: Long, query: String): List<Author>
    suspend fun findOrCreateAuthor(author: Author, userId: Long): Author
    suspend fun getAuthorById(userId: Long, id: Long): Author?
    suspend fun getAllAuthorsPaged(userId: Long, page: Int, pageSize: Int, sort: String? = null, desc: Boolean = false): PagedResponse<Author>
}

class AuthorServiceInterfaceImpl(
    private val authorRepository: AuthorRepository
) : AuthorServiceInterface {
    override suspend fun createAuthor(author: Author): Author {
        requireNotNull(author.userId) { "userId must not be null when creating an author" }
        return authorRepository.save(author)
    }

    override suspend fun searchAuthors(userId: Long, query: String): List<Author> =
        authorRepository.searchByName(userId, query)
            .map { it.copy(bookCount = authorRepository.countBooks(it.id!!)) }

    override suspend fun findOrCreateAuthor(author: Author, userId: Long): Author {
        // An explicit id wins (that is how two authors with the same name stay distinct), but only
        // if it is the caller's own author. Otherwise match this user's author by name.
        val existing = author.id?.let { authorRepository.findById(it) }?.takeIf { it.userId == userId }
            ?: authorRepository.findByName(userId, author.name)
        if (existing != null) return existing

        val now = LocalDateTime.now().toString()
        return authorRepository.save(
            author.copy(
                id = null,
                name = author.name.trim(),
                userId = userId,
                createdOn = now,
                createdBy = userId,
                modifiedOn = now,
                modifiedBy = userId
            )
        )
    }

    override suspend fun getAuthorById(userId: Long, id: Long): Author? =
        authorRepository.findById(id)
            ?.takeIf { it.userId == userId }
            ?.let { it.copy(bookCount = authorRepository.countBooks(id)) }

    override suspend fun getAllAuthorsPaged(userId: Long, page: Int, pageSize: Int, sort: String?, desc: Boolean): PagedResponse<Author> {
        val items = authorRepository.findAllByUserIdPaged(userId, page, pageSize, sort, desc)
            .map { it.copy(bookCount = authorRepository.countBooks(it.id!!)) }
        val total = authorRepository.countByUserId(userId)
        return PagedResponse.of(items, page, pageSize, total)
    }
}
