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
        const val REFRESH_TOKEN_EXPIRES = 3600000 * 7
    }

    val verifier: JWTVerifier = JWT.require(Algorithm.HMAC256(config.secret))
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

    fun validate(token: String) {

    }

    fun generateToken(user: UserDTO): Pair<String, String> {
        val accessToken = signToken(user, ACCESS_TOKEN_EXPIRES)
        val refreshToken = signToken(user, REFRESH_TOKEN_EXPIRES)
        return Pair(accessToken, refreshToken)
    }

    private fun signToken(user: UserDTO, expireTime: Int): String = JWT.create()
        .withAudience(config.audience)
        .withIssuer(config.issuer)
        .withClaim("email", user.email)
        .withClaim("role", user.role.name)
        .withExpiresAt(Date(System.currentTimeMillis() + expireTime))
        .sign(Algorithm.HMAC256(config.secret))
}