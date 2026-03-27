package com.dw.routes.metrics

import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get


fun Route.metrics() {
    get("/metrics") {
        call.respondText("Metrics endpoint")
    }
    get("/healthz") {
        call.respondText("Healthy")
    }
}