package com.dw.routes.helloWorld

import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get


fun Route.helloWorld() {
    get("/") {
        call.respondText("Hello World!")
    }
}