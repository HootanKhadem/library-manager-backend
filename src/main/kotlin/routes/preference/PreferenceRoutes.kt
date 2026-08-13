package com.dw.routes.preference

import com.dw.model.dto.UserPreference
import com.dw.service.preference.UserPreferenceServiceInterface
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.preferenceRoutes(preferenceService: UserPreferenceServiceInterface) {
    route("/api/preferences") {
        get {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                ?: return@get call.respond(HttpStatusCode.Unauthorized)
            call.respond(preferenceService.getPreferences(userId))
        }

        put {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                ?: return@put call.respond(HttpStatusCode.Unauthorized)
            val body = call.receive<UserPreference>()
            try {
                call.respond(preferenceService.savePreferences(userId, body))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
        }
    }
}
