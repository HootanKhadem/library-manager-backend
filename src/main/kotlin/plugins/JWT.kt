package com.dw.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureJWT() {
    val jwtConfig = getJwtConfig()

    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtConfig.realm
            verifier {
                JWT.require(Algorithm.HMAC256(jwtConfig.secret))
                    .withIssuer(jwtConfig.issuer)
                    .withAudience(jwtConfig.audience)
                    .build()
            }
            validate {
                JWTPrincipal(it.payload)
            }
        }
    }
}