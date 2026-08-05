package com.dw.db.postgres.genre

import com.dw.db.GenreRepository
import com.dw.db.mapping.GenreDAO
import com.dw.db.mapping.GenreTable
import com.dw.db.withTransaction
import com.dw.model.dto.Genre
import org.jetbrains.exposed.v1.core.eq
import java.time.LocalDateTime

private val DEFAULT_GENRES = listOf(
    "Fiction", "Non-fiction", "Mystery", "Biography",
    "Art Theory", "Science", "History", "Self-Help"
)

class PSQLGenreRepository : GenreRepository {

    override suspend fun save(genre: Genre): Genre = withTransaction {
        val now = LocalDateTime.now().toString()
        GenreDAO.new {
            name = genre.name
            userId = genre.userId ?: 0L
            createdOn = genre.createdOn ?: now
            createdBy = genre.createdBy
            modifiedOn = genre.modifiedOn ?: now
            modifiedBy = genre.modifiedBy
        }.toGenreDto()
    }

    override suspend fun findByUserId(userId: Long): List<Genre> = withTransaction {
        GenreDAO.find { GenreTable.userId eq userId }.map { it.toGenreDto() }
    }

    override suspend fun findById(id: Long): Genre? = withTransaction {
        GenreDAO.findById(id)?.toGenreDto()
    }

    override suspend fun update(id: Long, genre: Genre): Genre? = withTransaction {
        GenreDAO.findById(id)?.apply {
            name = genre.name
            modifiedOn = LocalDateTime.now().toString()
            modifiedBy = genre.modifiedBy
        }?.toGenreDto()
    }

    override suspend fun delete(id: Long): Boolean = withTransaction {
        val dao = GenreDAO.findById(id) ?: return@withTransaction false
        dao.delete()
        true
    }

    override suspend fun seedDefaults(userId: Long) = withTransaction {
        val now = LocalDateTime.now().toString()
        DEFAULT_GENRES.forEach { genreName ->
            GenreDAO.new {
                name = genreName
                this.userId = userId
                createdOn = now
                createdBy = userId
                modifiedOn = now
                modifiedBy = userId
            }
        }
    }
}
