package com.dw.routes.aggregation

import com.dw.service.book.BookServiceInterface
import com.dw.service.dashboard.DashboardServiceInterface
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.countUserBooks(bookService: BookServiceInterface) {
    get("/count-user-books") {
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
            ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val count = bookService.countBooks(userId)
        call.respond(count)
    }
}

fun Route.dashboardRoutes(dashboardService: DashboardServiceInterface) {
    route("/dashboard") {
        get("/stats/books") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                ?: return@get call.respond(HttpStatusCode.Unauthorized)
            call.respond(dashboardService.getBookStats(userId))
        }

        get("/stats/lent-out") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                ?: return@get call.respond(HttpStatusCode.Unauthorized)
            call.respond(dashboardService.getLentOutStats(userId))
        }

        get("/stats/overdue") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                ?: return@get call.respond(HttpStatusCode.Unauthorized)
            call.respond(dashboardService.getOverdueStats(userId))
        }

        get("/recently-added") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 5
            call.respond(dashboardService.getRecentlyAdded(userId, limit))
        }

        get("/recent-activity") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 5
            call.respond(dashboardService.getRecentActivity(userId, limit))
        }
    }
}
