package com.dw

import com.dw.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

suspend fun Application.module() {
    configureHTTP()

    configureDependencyInjection()


    configureRouting()
    configureContentNegotiation()
    configureJWT()
}
