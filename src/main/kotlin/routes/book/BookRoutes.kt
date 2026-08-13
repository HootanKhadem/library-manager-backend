package com.dw.routes.book

import com.dw.model.dto.Book
import com.dw.routes.pagination.pageParams
import com.dw.service.book.BookHasLendingHistoryException
import com.dw.service.book.BookServiceInterface
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put

fun Route.bookRoutes(bookService: BookServiceInterface) {
    post("/api/book") {
        val book = call.receive<Book>()
        val created = bookService.createBook(book)
        call.respond(HttpStatusCode.Created, created)
    }

    get("/api/book") {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
            ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val (page, pageSize) = call.pageParams()
        call.respond(HttpStatusCode.OK, bookService.getAllBooksPaged(userId, page, pageSize))
    }

    get("/api/book/{id}") {
        val id = call.parameters["id"]?.toLongOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid book ID")
        val book = bookService.getBookById(id)
            ?: return@get call.respond(HttpStatusCode.NotFound, "Book not found")
        call.respond(HttpStatusCode.OK, book)
    }

    put("/api/book/{id}") {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
            ?: return@put call.respond(HttpStatusCode.Unauthorized)
        val id = call.parameters["id"]?.toLongOrNull()
            ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid book ID")
        val book = call.receive<Book>()
        val updated = bookService.updateBook(id, book, userId)
            ?: return@put call.respond(HttpStatusCode.NotFound, "Book not found")
        call.respond(HttpStatusCode.OK, updated)
    }

    delete("/api/book/{id}") {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
            ?: return@delete call.respond(HttpStatusCode.Unauthorized)
        val id = call.parameters["id"]?.toLongOrNull()
            ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid book ID")
        val deleted = try {
            bookService.deleteBook(id, userId)
        } catch (e: BookHasLendingHistoryException) {
            return@delete call.respond(HttpStatusCode.Conflict, "Book has lending history and cannot be deleted")
        }
        if (deleted) call.respond(HttpStatusCode.NoContent)
        else call.respond(HttpStatusCode.NotFound, "Book not found")
    }
}
