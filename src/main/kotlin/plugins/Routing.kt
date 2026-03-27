package com.dw.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        get("/healthz") {
            call.respondText("Healthy", status = HttpStatusCode(222, "Healthy"))
        }
    }
}
