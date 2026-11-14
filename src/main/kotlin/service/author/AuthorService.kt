package com.dw.service.author

import com.dw.db.AuthorRepository
import com.dw.model.dto.Author
import com.dw.model.dto.MergeAuthorsRequest
import com.dw.model.dto.MergePreview
import com.dw.model.dto.PagedResponse
import java.time.LocalDateTime


interface AuthorServiceInterface {
    suspend fun createAuthor(author: Author): Author
    suspend fun searchAuthors(userId: Long, query: String): List<Author>
    suspend fun findOrCreateAuthor(author: Author, userId: Long): Author
    suspend fun getAuthorById(userId: Long, id: Long): Author?
    suspend fun getAllAuthorsPaged(userId: Long, page: Int, pageSize: Int, sort: String? = null, desc: Boolean = false): PagedResponse<Author>

    /**
     * Moves all books of the source authors onto the target and deletes the sources.
     * @return the surviving author, or null if any author is missing or not the caller's.
     * @throws IllegalArgumentException if there is no source, or the target is among the sources.
     */
    suspend fun mergeAuthors(userId: Long, request: MergeAuthorsRequest): Author?

    /** Dry run of [mergeAuthors]: same validation, changes nothing. */
    suspend fun previewMerge(userId: Long, request: MergeAuthorsRequest): MergePreview?
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

    /** Validates the request; returns the distinct source ids, or null if an author is missing or not the caller's. */
    private suspend fun validatedSources(userId: Long, request: MergeAuthorsRequest): List<Long>? {
        val sourceIds = request.sourceIds.distinct()
        require(sourceIds.isNotEmpty()) { "At least one author to merge is required" }
        require(request.targetId !in sourceIds) { "The target author cannot also be a source" }
        require(request.name == null || request.name.isNotBlank()) { "Name must not be blank" }
        val owned = (sourceIds + request.targetId).all { authorRepository.findById(it)?.userId == userId }
        return sourceIds.takeIf { owned }
    }

    override suspend fun previewMerge(userId: Long, request: MergeAuthorsRequest): MergePreview? {
        val sourceIds = validatedSources(userId, request) ?: return null
        val books = authorRepository.findBookTitles(sourceIds + request.targetId)
        // A collision is the same title (ignoring case/spaces) under two different authors.
        val duplicates = books.groupBy { it.second.trim().lowercase() }
            .filterValues { group -> group.map { it.first }.distinct().size > 1 }
            .map { it.value.first().second.trim() }
            .sortedBy { it.lowercase() }
        return MergePreview(books.size.toLong(), duplicates)
    }

    override suspend fun mergeAuthors(userId: Long, request: MergeAuthorsRequest): Author? {
        val sourceIds = validatedSources(userId, request) ?: return null
        val newName = request.name?.trim()

        val merged = authorRepository.merge(request.targetId, sourceIds, newName, userId, LocalDateTime.now().toString())
        return merged.copy(bookCount = authorRepository.countBooks(request.targetId))
    }
}
