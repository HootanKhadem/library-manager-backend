package com.dw.plugins

import com.dw.routes.helloWorld.helloWorld
import com.dw.routes.metrics.metrics
import com.dw.routes.user.login
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        helloWorld()
        metrics()
        login()
    }
}
