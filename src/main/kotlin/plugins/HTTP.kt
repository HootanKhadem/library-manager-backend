package com.dw.plugins

import com.asyncapi.kotlinasyncapi.context.service.AsyncApiExtension
import com.asyncapi.kotlinasyncapi.ktor.AsyncApiPlugin
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureHTTP() {
    install(AsyncApiPlugin) {
        AsyncApiExtension.builder {
                info {
                title("Sample API")
                version("1.0.0")
            }
        }
    }

    val allowedOrigin = Url(environment.config.property("ktor.cors.allowedOrigin").getString())
    install(CORS) {
        allowCredentials = true
        allowHost(allowedOrigin.hostWithPortIfSpecified, schemes = listOf(allowedOrigin.protocol.name))
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
    }
}
