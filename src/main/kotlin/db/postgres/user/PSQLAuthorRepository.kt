package com.dw.db.postgres.user

import com.dw.db.AuthorRepository
import com.dw.db.mapping.AuthorDAO
import com.dw.db.mapping.AuthorTable
import com.dw.db.withTransaction
import com.dw.model.dto.Author
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase

class PSQLAuthorRepository : AuthorRepository {
    override suspend fun save(author: Author): Author = withTransaction {
        AuthorDAO.new {
            updateFromDto(author)
        }.toAuthorDto()
    }

    override suspend fun searchByName(name: String): List<Author> = withTransaction {
        AuthorDAO.find { AuthorTable.name.lowerCase() like "%${name.lowercase()}%" }
            .map { it.toAuthorDto() }
    }

    override suspend fun findByName(name: String): Author? = withTransaction {
        AuthorDAO.find { AuthorTable.name.lowerCase() eq name.lowercase() }
            .firstOrNull()
            ?.toAuthorDto()
    }

    override suspend fun findById(id: Long): Author? = withTransaction {
        AuthorDAO.findById(id)?.toAuthorDto()
    }

    override suspend fun findAllByUserIdPaged(userId: Long, page: Int, pageSize: Int): List<Author> = withTransaction {
        AuthorDAO.find { AuthorTable.userId eq userId }
            .orderBy(AuthorTable.id to SortOrder.ASC)
            .limit(pageSize)
            .offset((page - 1).toLong() * pageSize)
            .map { it.toAuthorDto() }
    }

    override suspend fun countByUserId(userId: Long): Long = withTransaction {
        AuthorDAO.find { AuthorTable.userId eq userId }.count()
    }
}
