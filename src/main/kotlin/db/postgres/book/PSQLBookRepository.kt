package com.dw.db.postgres.book

import com.dw.db.BookRepository
import com.dw.db.mapping.AuthorDAO
import com.dw.db.mapping.AuthorTable
import com.dw.db.mapping.BookDAO
import com.dw.db.mapping.BookTable
import com.dw.db.withTransaction
import com.dw.model.dto.Book
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.LocalDateTime

class PSQLBookRepository : BookRepository {
    override suspend fun save(book: Book): Book = withTransaction {
        val authorId = book.author.id
            ?: throw IllegalArgumentException("Author must have an ID when saving a book")
        val authorDAO = AuthorDAO.findById(authorId)
            ?: throw NoSuchElementException("Author not found with ID: $authorId")
        BookDAO.new {
            name = book.name
            author = authorDAO
            translator = book.translator
            pages = book.pages
            isbn = book.isbn
            publishedDate = book.publishedDate
            publisher = book.publisher
            quantity = book.quantity
            image = book.image
            backImage = book.backImage
            status = book.status ?: "OWNED"
            userId = book.userId
            createdOn = book.createdOn ?: LocalDateTime.now().toString()
            createdBy = book.createdBy
            modifiedOn = book.modifiedOn
            modifiedBy = book.modifiedBy
        }.toBookDto()
    }

    override suspend fun findById(id: Long): Book? = withTransaction {
        BookDAO.findById(id)?.toBookDto()
    }

    override suspend fun findAllByUserId(userId: Long): List<Book> = withTransaction {
        BookDAO.find { BookTable.userId eq userId }.map { it.toBookDto() }
    }

    override suspend fun findAllByUserIdPaged(
        userId: Long,
        page: Int,
        pageSize: Int,
        search: String?,
        status: String?,
    ): List<Book> = withTransaction {
        val query = (BookTable innerJoin AuthorTable)
            .selectAll()
            .where { bookFilter(userId, search, status) }
            .orderBy(BookTable.id to SortOrder.ASC)
            .limit(pageSize)
            .offset((page - 1).toLong() * pageSize)
        BookDAO.wrapRows(query).map { it.toBookDto() }
    }

    override suspend fun findAllByAuthorId(userId: Long, authorId: Long): List<Book> = withTransaction {
        BookDAO.find { (BookTable.userId eq userId) and (BookTable.author eq authorId) }
            .orderBy(BookTable.id to SortOrder.ASC)
            .map { it.toBookDto() }
    }

    override suspend fun update(id: Long, book: Book): Book? = withTransaction {
        val bookDAO = BookDAO.findById(id) ?: return@withTransaction null
        val authorId = book.author.id
            ?: throw IllegalArgumentException("Author must have an ID when updating a book")
        val authorDAO = AuthorDAO.findById(authorId)
            ?: throw NoSuchElementException("Author not found with ID: $authorId")
        bookDAO.updateFromDto(book, authorDAO)
        bookDAO.toBookDto()
    }

    override suspend fun delete(id: Long): Boolean = withTransaction {
        val bookDAO = BookDAO.findById(id) ?: return@withTransaction false
        bookDAO.delete()
        true
    }

    override suspend fun countByUserId(userId: Long, search: String?, status: String?): Long = withTransaction {
        (BookTable innerJoin AuthorTable)
            .selectAll()
            .where { bookFilter(userId, search, status) }
            .count()
    }

    override suspend fun findRecentlyAdded(userId: Long, limit: Int): List<Book> = withTransaction {
        BookDAO.find { BookTable.userId eq userId }
            .orderBy(BookTable.createdOn to SortOrder.DESC)
            .limit(limit)
            .map { it.toBookDto() }
    }

    override suspend fun countAddedThisMonth(userId: Long, yearMonth: String): Long = withTransaction {
        BookDAO.find {
            (BookTable.userId eq userId) and (BookTable.createdOn like "$yearMonth%")
        }.count()
    }

    private fun bookFilter(userId: Long, search: String?, status: String?): Op<Boolean> {
        var condition: Op<Boolean> = BookTable.userId eq userId

        val trimmedStatus = status?.trim()
        if (!trimmedStatus.isNullOrBlank()) {
            condition = condition and (BookTable.status eq trimmedStatus)
        }

        val trimmedSearch = search?.trim()
        if (!trimmedSearch.isNullOrBlank()) {
            val pattern = "%${escapeLike(trimmedSearch.lowercase())}%"
            condition = condition and (
                (BookTable.name.lowerCase() like pattern) or
                    (BookTable.isbn.lowerCase() like pattern) or
                    (AuthorTable.name.lowerCase() like pattern)
                )
        }

        return condition
    }

    private fun escapeLike(s: String): String =
        s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
