package com.dw.service.authentication

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.dw.model.dto.UserDTO
import com.dw.plugins.JwtConfig
import java.util.*


class JwtService(private val config: JwtConfig) {

    val verifier: JWTVerifier = JWT.require(Algorithm.HMAC256(config.secret))
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

    fun validate(token: String) {

    }

    fun generateToken(user: UserDTO): String {
        return JWT.create()
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .withClaim("email", user.email)
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000)) // 1 hour for now
            .sign(Algorithm.HMAC256(config.secret))
    }
}