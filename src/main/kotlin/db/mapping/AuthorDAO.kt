package com.dw.db.mapping

import com.dw.model.dto.Author
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass


object AuthorTable : LongIdTable("author") {
    val name = varchar("name", 255)
    val image = varchar("image", 255)
    val userId = long("user_id").nullable()
    val createdOn = varchar("created_on", 255).nullable()
    val createdBy = long("created_by").nullable()
    val modifiedOn = varchar("modified_on", 255).nullable()
    val modifiedBy = long("modified_by").nullable()
}

class AuthorDAO(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<AuthorDAO>(AuthorTable)

    var name by AuthorTable.name
    var image by AuthorTable.image
    var userId by AuthorTable.userId
    var createdOn by AuthorTable.createdOn
    var createdBy by AuthorTable.createdBy
    var modifiedOn by AuthorTable.modifiedOn
    var modifiedBy by AuthorTable.modifiedBy
    val books by BookDAO referrersOn BookTable.author

    fun toAuthorDto(): Author = Author(
        id = id.value,
        name = name,
        image = image,
        userId = userId,
        createdOn = createdOn,
        createdBy = createdBy,
        modifiedOn = modifiedOn,
        modifiedBy = modifiedBy
    )

    fun updateFromDto(dto: Author) {
        this.name = dto.name
        this.image = dto.image
        this.userId = dto.userId
        this.createdOn = dto.createdOn
        this.createdBy = dto.createdBy
        this.modifiedOn = dto.modifiedOn
        this.modifiedBy = dto.modifiedBy
    }
}
