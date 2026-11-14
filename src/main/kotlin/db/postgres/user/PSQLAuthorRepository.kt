package com.dw.db.postgres.user

import com.dw.db.AuthorRepository
import com.dw.db.mapping.AuthorDAO
import com.dw.db.mapping.AuthorTable
import com.dw.db.mapping.BookTable
import com.dw.db.withTransaction
import com.dw.model.dto.Author
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class PSQLAuthorRepository : AuthorRepository {
    override suspend fun save(author: Author): Author = withTransaction {
        AuthorDAO.new {
            updateFromDto(author)
        }.toAuthorDto()
    }

    // Authors belong to a user: lookups must never see (or attach books to) another user's authors.
    override suspend fun searchByName(userId: Long, name: String, limit: Int): List<Author> = withTransaction {
        val pattern = "%${escapeLike(name.trim().lowercase())}%"
        AuthorDAO.find { (AuthorTable.userId eq userId) and (AuthorTable.name.lowerCase() like pattern) }
            .orderBy(AuthorTable.name.lowerCase() to SortOrder.ASC, AuthorTable.id to SortOrder.ASC)
            .limit(limit)
            .map { it.toAuthorDto() }
    }

    override suspend fun findByName(userId: Long, name: String): Author? = withTransaction {
        AuthorDAO.find { (AuthorTable.userId eq userId) and (AuthorTable.name.lowerCase() eq name.trim().lowercase()) }
            .orderBy(AuthorTable.id to SortOrder.ASC)
            .firstOrNull()
            ?.toAuthorDto()
    }

    private fun escapeLike(s: String): String =
        s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    override suspend fun findById(id: Long): Author? = withTransaction {
        AuthorDAO.findById(id)?.toAuthorDto()
    }

    override suspend fun findAllByUserIdPaged(
        userId: Long,
        page: Int,
        pageSize: Int,
        sort: String?,
        desc: Boolean,
    ): List<Author> = withTransaction {
        val order = if (desc) SortOrder.DESC else SortOrder.ASC
        val offset = (page - 1).toLong() * pageSize
        if (sort == "bookCount") {
            // Order by the number of books: left join so authors with 0 books still appear.
            val bookCount = BookTable.id.count()
            val ids = AuthorTable
                .join(BookTable, JoinType.LEFT, AuthorTable.id, BookTable.author)
                .select(AuthorTable.id, bookCount)
                .where { AuthorTable.userId eq userId }
                .groupBy(AuthorTable.id)
                .orderBy(bookCount to order, AuthorTable.id to SortOrder.ASC)
                .limit(pageSize)
                .offset(offset)
                .map { it[AuthorTable.id].value }
            ids.mapNotNull { AuthorDAO.findById(it)?.toAuthorDto() }
        } else {
            val column: Expression<*>? = when (sort) {
                "name" -> AuthorTable.name.lowerCase()
                "createdOn" -> AuthorTable.createdOn
                else -> null
            }
            val orderBy = listOfNotNull(column?.let { it to order }) + (AuthorTable.id to SortOrder.ASC)
            AuthorDAO.find { AuthorTable.userId eq userId }
                .orderBy(*orderBy.toTypedArray())
                .limit(pageSize)
                .offset(offset)
                .map { it.toAuthorDto() }
        }
    }

    override suspend fun countBooks(authorId: Long): Long = withTransaction {
        BookTable.selectAll().where { BookTable.author eq authorId }.count()
    }

    override suspend fun countByUserId(userId: Long): Long = withTransaction {
        AuthorDAO.find { AuthorTable.userId eq userId }.count()
    }

    override suspend fun findBookTitles(authorIds: List<Long>): List<Pair<Long, String>> = withTransaction {
        BookTable.select(BookTable.author, BookTable.name)
            .where { BookTable.author inList authorIds }
            .map { it[BookTable.author].value to it[BookTable.name] }
    }

    override suspend fun merge(
        targetId: Long,
        sourceIds: List<Long>,
        name: String?,
        modifiedBy: Long,
        modifiedOn: String,
    ): Author = withTransaction {
        val target = AuthorDAO.findById(targetId) ?: throw NoSuchElementException("Author not found with ID: $targetId")
        BookTable.update({ BookTable.author inList sourceIds }) { it[author] = targetId }
        AuthorTable.deleteWhere { AuthorTable.id inList sourceIds }
        name?.let { target.name = it }
        target.modifiedBy = modifiedBy
        target.modifiedOn = modifiedOn
        target.toAuthorDto()
    }
}
