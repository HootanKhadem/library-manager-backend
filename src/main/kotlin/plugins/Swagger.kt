package com.dw.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.swagger.*

data class SwaggerAuthConfig(
    val path: String,
    val username: String,
    val password: String,
    val openApiFile: String
)

fun Application.getSwaggerAuthConfig(): SwaggerAuthConfig {
    val config = environment.config
    val path = config.propertyOrNull("ktor.swagger.path")?.getString() ?: "/docs"
    val username = config.propertyOrNull("ktor.swagger.username")?.getString() ?: "swagger"
    val password = config.propertyOrNull("ktor.swagger.password")?.getString() ?: "swagger"
    val openApiFile = config.propertyOrNull("ktor.swagger.openapi.file")?.getString()
        ?: "openapi/documentation.yaml"

    return SwaggerAuthConfig(
        path = path,
        username = username,
        password = password,
        openApiFile = openApiFile
    )
}

fun Application.configureSwagger() {
    val swaggerCfg = getSwaggerAuthConfig()

    // Ensure Authentication plugin has the Basic provider for Swagger
    authentication {
        basic("swagger-basic") {
            realm = "Swagger UI"
            validate { credentials ->
                if (credentials.name == swaggerCfg.username && credentials.password == swaggerCfg.password) {
                    UserIdPrincipal(credentials.name)
                } else null
            }
        }
    }

    // Protect Swagger UI with Basic Auth
    routing {
        authenticate("swagger-basic") {
            val path = swaggerCfg.path.trimStart('/')
            swaggerUI(path = path, swaggerFile = swaggerCfg.openApiFile)
        }
    }
}
