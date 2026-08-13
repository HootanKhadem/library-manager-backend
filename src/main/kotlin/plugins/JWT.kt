package com.dw.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureJWT() {
    val jwtConfig = getJwtConfig()

    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtConfig.realm
            authHeader { call ->
                val cookie = call.request.cookies["access_token"]
                if (cookie != null) {
                    return@authHeader HttpAuthHeader.Single("Bearer", cookie)
                }
                call.request.parseAuthorizationHeader()
            }
            verifier {
                JWT.require(Algorithm.HMAC256(jwtConfig.secret))
                    .withIssuer(jwtConfig.issuer)
                    .withAudience(jwtConfig.audience)
                    .build()
            }
            validate {
                val email = it.payload.getClaim("email").asString()
                if (!email.isNullOrEmpty()) {
                    JWTPrincipal(it.payload)
                } else {
                    null
                }
            }
        }
    }
}