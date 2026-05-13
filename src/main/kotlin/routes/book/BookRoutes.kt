package com.dw.routes.book

import com.dw.model.dto.Book
import com.dw.service.book.BookServiceInterface
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.bookRoutes(bookService: BookServiceInterface) {
    post("/api/book") {
        val book = call.receive<Book>()
        val created = bookService.createBook(book)
        call.respond(HttpStatusCode.Created, created)
    }

    get("/api/book/{id}") {
        val id = call.parameters["id"]?.toLongOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid book ID")
        val book = bookService.getBookById(id)
            ?: return@get call.respond(HttpStatusCode.NotFound, "Book not found")
        call.respond(HttpStatusCode.OK, book)
    }
}
