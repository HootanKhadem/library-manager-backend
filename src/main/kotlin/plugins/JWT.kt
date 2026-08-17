package com.dw.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.dw.service.authentication.JwtService
import com.dw.service.authentication.JwtService.Companion.ACCESS_TOKEN_EXPIRES
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.util.date.*
import java.util.*

fun Application.configureJWT() {
    val jwtConfig = getJwtConfig()
    val jwtService = JwtService(jwtConfig)

    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtConfig.realm
            authHeader { call ->
                val accessCookie = call.request.cookies[ACCESS_TOKEN_COOKIE]
                if (accessCookie != null &&
                    jwtService.verify(accessCookie)?.getClaim("type")?.asString() == JwtService.TYPE_ACCESS
                ) {
                    return@authHeader HttpAuthHeader.Single("Bearer", accessCookie)
                }

                val refreshCookie = call.request.cookies[REFRESH_TOKEN_COOKIE]
                val newAccessToken = refreshCookie?.let { jwtService.refreshAccessToken(it) }
                if (newAccessToken != null) {
                    call.response.cookies.appendAuthCookie(
                        ACCESS_TOKEN_COOKIE,
                        newAccessToken,
                        GMTDate(Date().time + ACCESS_TOKEN_EXPIRES)
                    )
                    return@authHeader HttpAuthHeader.Single("Bearer", newAccessToken)
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
                val type = it.payload.getClaim("type").asString()
                if (!email.isNullOrEmpty() && type == JwtService.TYPE_ACCESS) {
                    JWTPrincipal(it.payload)
                } else {
                    null
                }
            }
        }
    }
}
