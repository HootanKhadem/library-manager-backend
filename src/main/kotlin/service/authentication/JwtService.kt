package com.dw.service.authentication

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.dw.model.dto.UserDTO
import com.dw.plugins.JwtConfig
import java.util.*


class JwtService(private val config: JwtConfig) {

    companion object {
        const val ACCESS_TOKEN_EXPIRES = 3600000
        const val REFRESH_TOKEN_EXPIRES = 3600000 * 24 * 7
        const val TYPE_ACCESS = "access"
        const val TYPE_REFRESH = "refresh"
    }

    fun generateToken(user: UserDTO): Pair<String, String> {
        val accessToken = signToken(user, ACCESS_TOKEN_EXPIRES, TYPE_ACCESS)
        val refreshToken = signToken(user, REFRESH_TOKEN_EXPIRES, TYPE_REFRESH)
        return Pair(accessToken, refreshToken)
    }

    fun verify(token: String): DecodedJWT? = try {
        verifier().verify(token)
    } catch (e: JWTVerificationException) {
        null
    }

    fun refreshAccessToken(refreshToken: String): String? {
        val decoded = verify(refreshToken) ?: return null
        if (decoded.getClaim("type").asString() != TYPE_REFRESH) return null

        val email = decoded.getClaim("email").asString() ?: return null
        val role = decoded.getClaim("role").asString() ?: return null
        val userId = decoded.getClaim("userId").asLong() ?: return null

        return JWT.create()
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .withClaim("email", email)
            .withClaim("role", role)
            .withClaim("userId", userId)
            .withClaim("type", TYPE_ACCESS)
            .withExpiresAt(Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRES))
            .sign(Algorithm.HMAC256(config.secret))
    }

    private fun verifier() = JWT.require(Algorithm.HMAC256(config.secret))
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

    private fun signToken(user: UserDTO, expireTime: Int, type: String): String = JWT.create()
        .withAudience(config.audience)
        .withIssuer(config.issuer)
        .withClaim("email", user.email)
        .withClaim("role", user.role.name)
        .withClaim("userId", user.id)
        .withClaim("type", type)
        .withExpiresAt(Date(System.currentTimeMillis() + expireTime))
        .sign(Algorithm.HMAC256(config.secret))

    fun getAccessTokenExpireTime(): Int = ACCESS_TOKEN_EXPIRES
    fun getRefreshTokenExpireTime(): Int = REFRESH_TOKEN_EXPIRES
}
