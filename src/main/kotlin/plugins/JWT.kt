package com.dw.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureJWT() {
    val secret = environment.config.property("ktor.jwt.secret").getString()
    val issuer = environment.config.property("ktor.jwt.issuer").getString()
    val audience = environment.config.property("ktor.jwt.audience").getString()
    val myRealm = environment.config.property("ktor.jwt.realm").getString()

    install(Authentication) {
        jwt("auth-jwt") {
            realm = myRealm
            verifier { JWT.require(Algorithm.HMAC256(secret)).withIssuer(issuer).withAudience(audience).build() }
            validate {
                JWTPrincipal(it.payload)
            }
        }
    }
}