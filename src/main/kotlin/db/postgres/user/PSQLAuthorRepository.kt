package com.dw.db.postgres.user

import com.dw.db.AuthorRepository
import com.dw.db.mapping.AuthorDAO
import com.dw.db.mapping.AuthorTable
import com.dw.db.withTransaction
import com.dw.model.dto.Author
import io.ktor.server.config.ApplicationConfig
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase

class PSQLAuthorRepository(private val config: ApplicationConfig? = null) : AuthorRepository {
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
}
