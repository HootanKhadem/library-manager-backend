package com.dw.service.genre

import com.dw.db.GenreRepository
import com.dw.model.dto.Genre
import java.time.LocalDateTime

interface GenreServiceInterface {
    suspend fun getGenresByUserId(userId: Long): List<Genre>
    suspend fun createGenre(genre: Genre): Genre
    suspend fun updateGenre(id: Long, genre: Genre): Genre?
    suspend fun deleteGenre(id: Long): Boolean
}

class GenreServiceImpl(private val genreRepository: GenreRepository) : GenreServiceInterface {

    override suspend fun getGenresByUserId(userId: Long): List<Genre> =
        genreRepository.findByUserId(userId)

    override suspend fun createGenre(genre: Genre): Genre {
        val now = LocalDateTime.now().toString()
        return genreRepository.save(
            genre.copy(createdOn = now, createdBy = genre.userId, modifiedOn = now, modifiedBy = genre.userId)
        )
    }

    override suspend fun updateGenre(id: Long, genre: Genre): Genre? =
        genreRepository.update(
            id,
            genre.copy(modifiedOn = LocalDateTime.now().toString(), modifiedBy = genre.userId)
        )

    override suspend fun deleteGenre(id: Long): Boolean = genreRepository.delete(id)
}
