package com.dw.plugins

import com.dw.routes.helloWorld.helloWorld
import com.dw.routes.metrics.metrics
import com.dw.routes.user.login
import com.dw.service.authentication.LoginServiceInterface
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.routing.*

suspend fun Application.configureRouting() {

    val loginService = dependencies.resolve<LoginServiceInterface>()

    routing {
        helloWorld()
        metrics()
        login(loginService)
    }
}
