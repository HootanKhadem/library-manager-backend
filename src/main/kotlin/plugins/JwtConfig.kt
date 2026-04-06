package com.dw.plugins

import io.ktor.server.application.*

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String
)

fun Application.getJwtConfig(): JwtConfig {
    return JwtConfig(
        secret = environment.config.property("ktor.jwt.secret").getString(),
        issuer = environment.config.property("ktor.jwt.issuer").getString(),
        audience = environment.config.property("ktor.jwt.audience").getString(),
        realm = environment.config.property("ktor.jwt.realm").getString()
    )
}
