package com.dw.service.book

import com.dw.db.BookRepository
import com.dw.db.LendingRepository
import com.dw.model.dto.Book
import com.dw.model.dto.PagedResponse
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
    suspend fun getAllBooksPaged(userId: Long, page: Int, pageSize: Int): PagedResponse<Book>
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
    private val lendingRepository: LendingRepository
) : BookServiceInterface {
    override suspend fun getBookById(id: Long): Book? = bookRepository.findById(id)

    override suspend fun getAllBooks(userId: Long): List<Book> = bookRepository.findAllByUserId(userId)

    override suspend fun getAllBooksPaged(userId: Long, page: Int, pageSize: Int): PagedResponse<Book> {
        val items = bookRepository.findAllByUserIdPaged(userId, page, pageSize)
        val total = bookRepository.countByUserId(userId)
        return PagedResponse.of(items, page, pageSize, total)
    }

    override suspend fun createBook(book: Book): Book {
        val resolvedAuthor = authorService.findOrCreateAuthor(book.author, book.userId)
        val now = LocalDateTime.now().toString()
        return bookRepository.save(
            book.copy(
                author = resolvedAuthor,
                createdOn = now,
                createdBy = book.userId,
                modifiedOn = now,
                modifiedBy = book.userId
            )
        )
    }

    override suspend fun updateBook(id: Long, book: Book, requesterId: Long): Book? {
        val existing = bookRepository.findById(id) ?: return null
        if (existing.userId != requesterId) return null
        // Ownership must not be transferable via the request body: pin userId/modifiedBy to the
        // verified owner, regardless of what the caller put in the body.
        val resolvedAuthor = authorService.findOrCreateAuthor(book.author, existing.userId)
        return bookRepository.update(
            id,
            book.copy(author = resolvedAuthor, userId = existing.userId, modifiedBy = requesterId)
        )
    }

    override suspend fun deleteBook(id: Long, requesterId: Long): Boolean {
        val existing = bookRepository.findById(id) ?: return false
        if (existing.userId != requesterId) return false
        if (lendingRepository.existsByBookId(id)) throw BookHasLendingHistoryException(id)
        return bookRepository.delete(id)
    }
    override suspend fun countBooks(userId: Long): Long = bookRepository.countByUserId(userId)
}
