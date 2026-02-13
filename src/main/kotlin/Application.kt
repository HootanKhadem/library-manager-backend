package com.dw

import com.dw.plugins.configureHTTP
import com.dw.plugins.configureRouting
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureHTTP()
    configureRouting()
}
