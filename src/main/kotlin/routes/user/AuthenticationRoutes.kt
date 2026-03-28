package com.dw.routes.user

import com.dw.service.authentication.LoginServiceInterface
import io.ktor.server.routing.*

fun Route.login(loginService: LoginServiceInterface) {
    post("/auth/login") {
        loginService.login(call.parameters["username"]!!, call.parameters["password"]!!)
    }
}