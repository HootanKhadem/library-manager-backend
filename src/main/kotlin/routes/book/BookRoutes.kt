package com.dw.routes.book

import com.dw.model.dto.Book
import com.dw.model.dto.openlib.BookLookupDto
import com.dw.routes.pagination.pageParams
import com.dw.service.book.BookHasLendingHistoryException
import com.dw.service.book.BookServiceInterface
import com.dw.service.openLib.OpenLibApi
import io.ktor.http.HttpStatusCode
import org.slf4j.LoggerFactory
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put

private val log = LoggerFactory.getLogger("com.dw.routes.book.BookRoutes")

fun Route.bookRoutes(bookService: BookServiceInterface, openLibApi: OpenLibApi) {
    get("/api/book/isbn/{isbn}") {
        val isbn = call.parameters["isbn"]?.takeIf { it.isNotBlank() }
            ?: return@get call.respond(HttpStatusCode.BadRequest, "ISBN must not be blank")

        val doc = try {
            openLibApi.getBookInfo(isbn)
        } catch (e: Exception) {
            log.warn("ISBN lookup failed for '{}': {}", isbn, e.message)
            return@get call.respond(HttpStatusCode.NotFound, "No book found for this ISBN")
        }

        call.respond(
            HttpStatusCode.OK,
            BookLookupDto(
                title = doc.title,
                author = doc.authorName.firstOrNull(),
                publisher = doc.publisher.firstOrNull(),
                publishedDate = doc.firstPublishYear?.toString(),
                pages = doc.numberOfPagesMedian
            )
        )
    }

    post("/api/book") {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
            ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val book = call.receive<Book>()
        val created = bookService.createBook(book.copy(userId = userId))
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
