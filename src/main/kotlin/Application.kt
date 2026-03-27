package com.dw

import com.dw.plugins.configureContentNegotiation
import com.dw.plugins.configureHTTP
import com.dw.plugins.configureJWT
import com.dw.plugins.configureRouting
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureHTTP()
    configureRouting()
    configureContentNegotiation()
    configureJWT()
}
