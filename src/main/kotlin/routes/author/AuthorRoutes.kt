package com.dw.routes.author

import com.dw.model.dto.Author
import com.dw.routes.pagination.pageParams
import com.dw.routes.pagination.sortParams
import com.dw.service.author.AuthorServiceInterface
import com.dw.service.book.BookServiceInterface
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.authorRoutes(authorServiceInterface: AuthorServiceInterface, bookService: BookServiceInterface) {
    post("/api/author") {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
            ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val author = call.receive<Author>()
        val createdAuthor = authorServiceInterface.createAuthor(author.copy(userId = userId))
        call.respond(HttpStatusCode.Created, createdAuthor)
    }

    get("/api/author") {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
            ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val (page, pageSize) = call.pageParams()
        val (sort, desc) = call.sortParams()
        call.respond(HttpStatusCode.OK, authorServiceInterface.getAllAuthorsPaged(userId, page, pageSize, sort, desc))
    }

    get("/api/author/{id}") {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
            ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val id = call.parameters["id"]?.toLongOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid author ID")
        val author = authorServiceInterface.getAuthorById(userId, id)
            ?: return@get call.respond(HttpStatusCode.NotFound, "Author not found")
        call.respond(HttpStatusCode.OK, author)
    }

    get("/api/author/{id}/books") {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
            ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val authorId = call.parameters["id"]?.toLongOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid author ID")
        call.respond(HttpStatusCode.OK, bookService.getBooksByAuthor(userId, authorId))
    }

    get("/api/author/search") {
        val query = call.request.queryParameters["query"] ?: ""
        val authors = authorServiceInterface.searchAuthors(query)
        call.respond(HttpStatusCode.OK, authors)
    }
}
