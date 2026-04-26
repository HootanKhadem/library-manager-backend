package com.dw.routes.user

import com.dw.model.dto.LoginDTO
import com.dw.service.authentication.JwtService.Companion.ACCESS_TOKEN_EXPIRES
import com.dw.service.authentication.LoginServiceInterface
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.date.*
import java.util.*

fun Route.login(loginService: LoginServiceInterface) {
    post("/auth/login") {
        val loginDTO = call.receive<LoginDTO>()
        val token = loginService.login(loginDTO.username, loginDTO.password)
        call.response.cookies.append("access_token", token.first, secure = true, expires = GMTDate(Date().time + ACCESS_TOKEN_EXPIRES))
        call.response.cookies.append("refresh_token", token.second, secure = true, expires = GMTDate(Date().time + ACCESS_TOKEN_EXPIRES))
        call.respond(mapOf("access_token" to token.first, "refresh_token" to token.second))
    }
}