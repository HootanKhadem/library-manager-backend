package com.dw.service.book

import com.dw.db.BookRepository
import com.dw.db.LendingRepository
import com.dw.db.UserActivityLogRepository
import com.dw.model.dto.Book
import com.dw.model.dto.ActivityAction
import com.dw.model.dto.PagedResponse
import com.dw.model.dto.UserActivityLog
import com.dw.service.author.AuthorServiceInterface
import java.time.LocalDateTime

/**
 * Thrown by [BookServiceInterface.deleteBook] when the book cannot be deleted because it has
 * lending history (the `lending.book_id` foreign key has no ON DELETE clause). Routes should
 * catch this and respond 409 Conflict.
 */
class BookHasLendingHistoryException(val bookId: Long) :
    Exception("Book $bookId cannot be deleted because it has lending history")

interface BookServiceInterface {
    suspend fun getBookById(id: Long): Book?
    suspend fun getAllBooks(userId: Long): List<Book>
    suspend fun getAllBooksPaged(
        userId: Long,
        page: Int,
        pageSize: Int,
        search: String? = null,
        status: String? = null,
        sort: String? = null,
        desc: Boolean = false
    ): PagedResponse<Book>
    suspend fun getBooksByAuthor(userId: Long, authorId: Long): List<Book>
    suspend fun createBook(book: Book): Book
    suspend fun updateBook(id: Long, book: Book, requesterId: Long): Book?

    /**
     * @throws BookHasLendingHistoryException if the book has lending history and cannot be deleted.
     */
    suspend fun deleteBook(id: Long, requesterId: Long): Boolean
    suspend fun countBooks(userId: Long): Long
}

class BookServiceImpl(
    private val bookRepository: BookRepository,
    private val authorService: AuthorServiceInterface,
    private val lendingRepository: LendingRepository,
    private val activityLogRepository: UserActivityLogRepository
) : BookServiceInterface {
    override suspend fun getBookById(id: Long): Book? = bookRepository.findById(id)

    override suspend fun getAllBooks(userId: Long): List<Book> = bookRepository.findAllByUserId(userId)

    override suspend fun getAllBooksPaged(
        userId: Long,
        page: Int,
        pageSize: Int,
        search: String?,
        status: String?,
        sort: String?,
        desc: Boolean
    ): PagedResponse<Book> {
        // List rows omit the (large, inline) cover images; GET /api/book/{id} returns them.
        val items = bookRepository.findAllByUserIdPaged(userId, page, pageSize, search, status, sort, desc)
            .map { it.copy(image = null, backImage = null) }
        val total = bookRepository.countByUserId(userId, search, status)
        return PagedResponse.of(items, page, pageSize, total)
    }

    override suspend fun getBooksByAuthor(userId: Long, authorId: Long): List<Book> =
        bookRepository.findAllByAuthorId(userId, authorId)

    private suspend fun logActivity(action: ActivityAction, userId: Long, book: Book) {
        activityLogRepository.save(UserActivityLog(userId = userId, action = action.name, bookId = book.id, bookName = book.name))
    }

    override suspend fun createBook(book: Book): Book {
        val userId = requireNotNull(book.userId) { "userId must not be null when creating a book" }
        val resolvedAuthor = authorService.findOrCreateAuthor(book.author, userId)
        val now = LocalDateTime.now().toString()
        val saved = bookRepository.save(
            book.copy(
                author = resolvedAuthor,
                userId = userId,
                createdOn = now,
                createdBy = userId,
                modifiedOn = now,
                modifiedBy = userId
            )
        )
        logActivity(ActivityAction.ADDED, userId, saved)
        return saved
    }

    override suspend fun updateBook(id: Long, book: Book, requesterId: Long): Book? {
        val existing = bookRepository.findById(id) ?: return null
        if (existing.userId != requesterId) return null
        // Ownership must not be transferable via the request body: pin userId/modifiedBy to the
        // verified owner, regardless of what the caller put in the body.
        val resolvedAuthor = authorService.findOrCreateAuthor(book.author, existing.userId)
        val updated = bookRepository.update(
            id,
            book.copy(author = resolvedAuthor, userId = existing.userId, modifiedBy = requesterId)
        )
        updated?.let { logActivity(ActivityAction.UPDATED, requesterId, it) }
        return updated
    }

    override suspend fun deleteBook(id: Long, requesterId: Long): Boolean {
        val existing = bookRepository.findById(id) ?: return false
        if (existing.userId != requesterId) return false
        if (lendingRepository.existsByBookId(id)) throw BookHasLendingHistoryException(id)
        val deleted = bookRepository.delete(id)
        if (deleted) logActivity(ActivityAction.REMOVED, requesterId, existing)
        return deleted
    }
    override suspend fun countBooks(userId: Long): Long = bookRepository.countByUserId(userId)
}
