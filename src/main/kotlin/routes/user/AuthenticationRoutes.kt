package com.dw.routes.user

import com.auth0.jwt.JWT
import com.dw.EmailAlreadyExistsException
import com.dw.model.dto.LoginDTO
import com.dw.model.dto.SignupRequest
import com.dw.plugins.ACCESS_TOKEN_COOKIE
import com.dw.plugins.REFRESH_TOKEN_COOKIE
import com.dw.plugins.appendAuthCookie
import com.dw.plugins.clearAuthCookie
import com.dw.service.authentication.JwtService.Companion.ACCESS_TOKEN_EXPIRES
import com.dw.service.authentication.JwtService.Companion.REFRESH_TOKEN_EXPIRES
import com.dw.service.authentication.LoginServiceInterface
import com.dw.service.authentication.SignupServiceInterface
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.date.*
import java.util.*

fun Route.login(loginService: LoginServiceInterface) {
    post("/auth/login") {
        val loginDTO = call.receive<LoginDTO>()
        val token = loginService.login(loginDTO.email, loginDTO.password)
        appendTokensToCookies(token)

        val accessClaims = JWT.decode(token.first)
        call.respond(
            mapOf(
                "email" to accessClaims.getClaim("email").asString(),
                "role" to accessClaims.getClaim("role").asString()
            )
        )
    }
}

fun Route.signup(signupService: SignupServiceInterface) {
    post("/auth/signup") {
        val request = call.receive<SignupRequest>()
        try {
            val (user, tokens) = signupService.signup(request)
            appendTokensToCookies(tokens)
            call.respond(
                HttpStatusCode.Created,
                mapOf("name" to user.name, "email" to user.email, "role" to user.role.name)
            )
        } catch (e: EmailAlreadyExistsException) {
            call.respond(HttpStatusCode.Conflict, mapOf("error" to e.message))
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
        }
    }
}

fun Route.logout() {
    post("/auth/logout") {
        call.response.cookies.clearAuthCookie(ACCESS_TOKEN_COOKIE)
        call.response.cookies.clearAuthCookie(REFRESH_TOKEN_COOKIE)
        call.respond(mapOf("message" to "logged out"))
    }
}

private fun RoutingContext.appendTokensToCookies(token: Pair<String, String>) {
    call.response.cookies.appendAuthCookie(
        ACCESS_TOKEN_COOKIE,
        token.first,
        GMTDate(Date().time + ACCESS_TOKEN_EXPIRES)
    )
    call.response.cookies.appendAuthCookie(
        REFRESH_TOKEN_COOKIE,
        token.second,
        GMTDate(Date().time + REFRESH_TOKEN_EXPIRES)
    )
}
