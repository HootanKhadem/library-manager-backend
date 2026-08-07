package com.dw.service.book

import com.dw.db.BookRepository
import com.dw.model.dto.Book
import com.dw.model.dto.PagedResponse
import com.dw.service.author.AuthorServiceInterface
import java.time.LocalDateTime

interface BookServiceInterface {
    suspend fun getBookById(id: Long): Book?
    suspend fun getAllBooks(userId: Long): List<Book>
    suspend fun getAllBooksPaged(userId: Long, page: Int, pageSize: Int): PagedResponse<Book>
    suspend fun createBook(book: Book): Book
    suspend fun updateBook(id: Long, book: Book): Book?
    suspend fun deleteBook(id: Long): Boolean
    suspend fun countBooks(userId: Long): Long
}

class BookServiceImpl(
    private val bookRepository: BookRepository,
    private val authorService: AuthorServiceInterface
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

    override suspend fun updateBook(id: Long, book: Book): Book? {
        val resolvedAuthor = authorService.findOrCreateAuthor(book.author, book.userId)
        return bookRepository.update(id, book.copy(author = resolvedAuthor))
    }

    override suspend fun deleteBook(id: Long): Boolean = bookRepository.delete(id)
    override suspend fun countBooks(userId: Long): Long = bookRepository.countByUserId(userId)
}
