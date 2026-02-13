package com.dw.plugins

import com.asyncapi.kotlinasyncapi.context.service.AsyncApiExtension
import com.asyncapi.kotlinasyncapi.ktor.AsyncApiPlugin
import io.ktor.server.application.*

fun Application.configureHTTP() {
    install(AsyncApiPlugin) {
        extension = AsyncApiExtension.builder {
            info {
                title("Sample API")
                version("1.0.0")
            }
        }
    }
}
