package com.dw.routes.genre

import com.dw.model.dto.Genre
import com.dw.service.genre.GenreServiceInterface
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
import io.ktor.server.routing.route

fun Route.genreRoutes(genreService: GenreServiceInterface) {
    route("/api/genre") {
        get {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                ?: return@get call.respond(HttpStatusCode.Unauthorized)
            call.respond(genreService.getGenresByUserId(userId))
        }

        post {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val body = call.receive<Genre>()
            val created = genreService.createGenre(body.copy(userId = userId))
            call.respond(HttpStatusCode.Created, created)
        }

        put("/{id}") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                ?: return@put call.respond(HttpStatusCode.Unauthorized)
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest)
            val body = call.receive<Genre>()
            val updated = genreService.updateGenre(id, body.copy(userId = userId))
                ?: return@put call.respond(HttpStatusCode.NotFound)
            call.respond(updated)
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val deleted = genreService.deleteGenre(id)
            if (deleted) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound)
        }
    }
}
