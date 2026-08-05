package com.dw.db.mapping

import com.dw.model.dto.Book
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import java.time.LocalDateTime


object BookTable : LongIdTable("book") {
    val name = varchar("name", 255)
    val author = reference("author_id", AuthorTable)
    val translator = varchar("translator", 255).nullable()
    val pages = integer("pages")
    val isbn = varchar("isbn", 255).uniqueIndex()
    val publishedDate = varchar("published_date", 255)
    val publisher = varchar("publisher", 255)
    val quantity = integer("quantity")
    val image = varchar("image", 255).nullable()
    val userId = long("user_id").nullable()
    val createdOn = varchar("created_on", 255).nullable()
    val createdBy = long("created_by").nullable()
    val modifiedOn = varchar("modified_on", 255).nullable()
    val modifiedBy = long("modified_by").nullable()
    val genreId = long("genre_id").nullable()
    val rating = integer("rating").nullable()
    val status = varchar("status", 50).nullable().default("OWNED")
}

class BookDAO(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<BookDAO>(BookTable)

    var name by BookTable.name
    var author by AuthorDAO referencedOn BookTable.author
    var translator by BookTable.translator
    var pages by BookTable.pages
    var isbn by BookTable.isbn
    var publishedDate by BookTable.publishedDate
    var publisher by BookTable.publisher
    var quantity by BookTable.quantity
    var image by BookTable.image
    var userId by BookTable.userId
    var createdOn by BookTable.createdOn
    var createdBy by BookTable.createdBy
    var modifiedOn by BookTable.modifiedOn
    var modifiedBy by BookTable.modifiedBy
    var genreId by BookTable.genreId
    var rating by BookTable.rating
    var status by BookTable.status

    fun toBookDto(): Book = Book(
        id = id.value,
        name = name,
        author = author.toAuthorDto(),
        translator = translator,
        pages = pages,
        isbn = isbn,
        publishedDate = publishedDate,
        publisher = publisher,
        quantity = quantity,
        image = image,
        genreId = genreId,
        rating = rating,
        status = status,
        userId = userId,
        createdOn = createdOn,
        createdBy = createdBy,
        modifiedOn = modifiedOn,
        modifiedBy = modifiedBy
    )

    fun updateFromDto(dto: Book, authorDAO: AuthorDAO) {
        this.name = dto.name
        this.author = authorDAO
        this.translator = dto.translator
        this.pages = dto.pages
        this.isbn = dto.isbn
        this.publishedDate = dto.publishedDate
        this.publisher = dto.publisher
        this.quantity = dto.quantity
        this.image = dto.image
        this.genreId = dto.genreId
        this.rating = dto.rating
        this.status = dto.status
        this.userId = dto.userId
        this.modifiedOn = LocalDateTime.now().toString()
        this.modifiedBy = dto.modifiedBy
    }
}
