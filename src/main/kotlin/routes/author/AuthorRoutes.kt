package com.dw.routes.author

import com.dw.model.dto.Author
import com.dw.service.author.AuthorServiceInterface
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.authorRoutes(authorServiceInterface: AuthorServiceInterface) {
    post("/api/author") {
        val author = call.receive<Author>()
        val createdAuthor = authorServiceInterface.createAuthor(author)
        call.respond(HttpStatusCode.Created, createdAuthor)
    }

    get("/api/author/search") {
        val query = call.request.queryParameters["query"] ?: ""
        val authors = authorServiceInterface.searchAuthors(query)
        call.respond(HttpStatusCode.OK, authors)
    }
}