package com.dw

import com.dw.db.UserRepository
import com.dw.plugins.*
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

suspend fun Application.module() {
    configureHTTP()
    configureDatabases()

    configureDependencyInjection()
    dependencies.resolve<UserRepository>().createAdminUser()

    configureJWT()
    configureSwagger()
    configurePublicRouting()
    configureAuthenticatedRouting()
    configureAdminRouting()
    configureContentNegotiation()
}
