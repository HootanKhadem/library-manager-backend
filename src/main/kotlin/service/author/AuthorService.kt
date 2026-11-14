package com.dw.service.author

import com.dw.db.AuthorRepository
import com.dw.model.dto.Author
import com.dw.model.dto.PagedResponse
import java.time.LocalDateTime


interface AuthorServiceInterface {
    suspend fun createAuthor(author: Author): Author
    suspend fun searchAuthors(query: String): List<Author>
    suspend fun findOrCreateAuthor(author: Author, userId: Long? = null): Author
    suspend fun getAuthorById(userId: Long, id: Long): Author?
    suspend fun getAllAuthorsPaged(userId: Long, page: Int, pageSize: Int): PagedResponse<Author>
}

class AuthorServiceInterfaceImpl(
    private val authorRepository: AuthorRepository
) : AuthorServiceInterface {
    override suspend fun createAuthor(author: Author): Author {
        requireNotNull(author.userId) { "userId must not be null when creating an author" }
        return authorRepository.save(author)
    }

    override suspend fun searchAuthors(query: String): List<Author> {
        return authorRepository.searchByName(query)
    }

    override suspend fun findOrCreateAuthor(author: Author, userId: Long?): Author {
        val existing = author.id?.let { authorRepository.findById(it) }
            ?: authorRepository.findByName(author.name)
        if (existing != null) return existing

        requireNotNull(userId) { "userId must not be null when creating an author" }
        val now = LocalDateTime.now().toString()
        return authorRepository.save(
            author.copy(
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

    override suspend fun getAllAuthorsPaged(userId: Long, page: Int, pageSize: Int): PagedResponse<Author> {
        val items = authorRepository.findAllByUserIdPaged(userId, page, pageSize)
            .map { it.copy(bookCount = authorRepository.countBooks(it.id!!)) }
        val total = authorRepository.countByUserId(userId)
        return PagedResponse.of(items, page, pageSize, total)
    }
}
