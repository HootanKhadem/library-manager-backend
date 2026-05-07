package com.dw.service.author

import com.dw.db.AuthorRepository
import com.dw.model.dto.Author
import java.time.LocalDateTime


interface AuthorServiceInterface {
    suspend fun createAuthor(author: Author): Author
    suspend fun searchAuthors(query: String): List<Author>
    suspend fun findOrCreateAuthor(author: Author, userId: Long? = null): Author
}

class AuthorServiceInterfaceImpl(
    private val authorRepository: AuthorRepository
) : AuthorServiceInterface {
    override suspend fun createAuthor(author: Author): Author {
        return authorRepository.save(author)
    }

    override suspend fun searchAuthors(query: String): List<Author> {
        return authorRepository.searchByName(query)
    }

    override suspend fun findOrCreateAuthor(author: Author, userId: Long?): Author {
        val existing = author.id?.let { authorRepository.findById(it) }
            ?: authorRepository.findByName(author.name)
        if (existing != null) return existing

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
}
