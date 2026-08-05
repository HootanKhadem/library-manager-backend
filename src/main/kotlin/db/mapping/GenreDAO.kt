package com.dw.db.mapping

import com.dw.model.dto.Genre
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

object GenreTable : LongIdTable("genre") {
    val name = varchar("name", 255)
    val userId = long("user_id")
    val createdOn = varchar("created_on", 255).nullable()
    val createdBy = long("created_by").nullable()
    val modifiedOn = varchar("modified_on", 255).nullable()
    val modifiedBy = long("modified_by").nullable()
}

class GenreDAO(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<GenreDAO>(GenreTable)

    var name by GenreTable.name
    var userId by GenreTable.userId
    var createdOn by GenreTable.createdOn
    var createdBy by GenreTable.createdBy
    var modifiedOn by GenreTable.modifiedOn
    var modifiedBy by GenreTable.modifiedBy

    fun toGenreDto(): Genre = Genre(
        id = id.value,
        name = name,
        userId = userId,
        createdOn = createdOn,
        createdBy = createdBy,
        modifiedOn = modifiedOn,
        modifiedBy = modifiedBy
    )
}
