package com.dw.plugins

import com.dw.model.dto.Role
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class RoleAuthorizationConfig {
    var requiredRole: Role? = null
}

val RoleAuthorizationPlugin = createRouteScopedPlugin(
    name = "RoleAuthorizationPlugin",
    createConfiguration = ::RoleAuthorizationConfig
) {
    val requiredRole = pluginConfig.requiredRole ?: return@createRouteScopedPlugin

    on(AuthenticationChecked) { call ->
        val principal = call.principal<JWTPrincipal>()
        if (principal == null) {
            // Principal might be missing if authentication failed or wasn't required
            // But usually withRole is used inside authenticate { ... }
            return@on
        }

        val userRoleStr = principal.payload.getClaim("role").asString()
        val userRole = userRoleStr?.let {
            try { Role.valueOf(it) } catch (e: Exception) { null }
        }

        if (userRole != requiredRole) {
            call.respond(HttpStatusCode.Forbidden, "You do not have the required role.")
        }
    }
}

fun Route.withRole(role: Role, build: Route.() -> Unit): Route {
    val authorizedRoute = createChild(object : RouteSelector() {
        override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation =
            RouteSelectorEvaluation.Constant
    })
    authorizedRoute.install(RoleAuthorizationPlugin) {
        requiredRole = role
    }
    authorizedRoute.build()
    return authorizedRoute
}
