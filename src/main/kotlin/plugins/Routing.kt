package com.dw.plugins

import com.dw.model.dto.Role
import com.dw.routes.admin.adminCreateUser
import com.dw.routes.helloWorld.helloWorld
import com.dw.routes.metrics.metrics
import com.dw.routes.user.login
import com.dw.service.admin.CreateUserServiceInterface
import com.dw.service.authentication.LoginServiceInterface
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.di.*
import io.ktor.server.routing.*

suspend fun Application.configurePublicRouting() {

    val loginService = dependencies.resolve<LoginServiceInterface>()

    routing {
        helloWorld()
        metrics()
        login(loginService)
    }
}


fun Application.configureAuthenticatedRouting() {

    routing {
        authenticate("auth-jwt") {
             withRole(Role.USER) {
                 // TODO: Add authenticated routes for user role here
             }
        }
    }
}

suspend fun Application.configureAdminRouting() {
    val adminCreateUserService = dependencies.resolve<CreateUserServiceInterface>()

    routing {
        authenticate("auth-jwt") {
            withRole(Role.ADMIN) {
                adminCreateUser(adminCreateUserService)
            }
        }
    }
}