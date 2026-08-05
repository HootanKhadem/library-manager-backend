package com.dw.routes.lending

import com.dw.model.dto.Lending
import com.dw.service.lending.LendingServiceInterface
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.lendingRoutes(lendingService: LendingServiceInterface) {
    route("/api/lending") {
        post {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val body = call.receive<Lending>()
            val saved = lendingService.lendBook(body.copy(userId = userId))
            call.respond(HttpStatusCode.Created, saved)
        }

        get("/active") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                ?: return@get call.respond(HttpStatusCode.Unauthorized)
            call.respond(lendingService.getActiveByUserId(userId))
        }

        put("/{id}/return") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                ?: return@put call.respond(HttpStatusCode.Unauthorized)
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest)
            val returned = lendingService.returnBook(id, userId)
                ?: return@put call.respond(HttpStatusCode.NotFound)
            call.respond(returned)
        }
    }
}
