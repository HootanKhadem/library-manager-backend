package com.dw.routes.user

import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.login() {
    post("/auth/login") {  }
}