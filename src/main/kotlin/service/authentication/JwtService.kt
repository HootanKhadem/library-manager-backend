package com.dw.service.authentication

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.dw.model.dto.UserDTO
import com.dw.plugins.JwtConfig
import java.util.*


class JwtService(private val config: JwtConfig) {

    companion object {
        const val ACCESS_TOKEN_EXPIRES = 3600000
    }

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
            .withClaim("role", user.role.name)
            .withExpiresAt(Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRES)) // 1 hour for now
            .sign(Algorithm.HMAC256(config.secret))
    }
}