package com.dw.service.authentication

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.dw.model.dto.Role
import com.dw.model.dto.UserDTO
import com.dw.plugins.JwtConfig
import com.dw.service.authentication.JwtService.Companion.ACCESS_TOKEN_EXPIRES
import com.dw.service.authentication.JwtService.Companion.REFRESH_TOKEN_EXPIRES
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JwtServiceTest {

    private val config = JwtConfig(
        secret = "test-secret",
        issuer = "test-issuer",
        audience = "test-audience",
        realm = "test-realm"
    )
    private val jwtService = JwtService(config)

    @Test
    fun `generateToken should return valid access and refresh JWT`() {
        // Given
        val user = UserDTO(
            id = 1,
            name = "Test User",
            email = "test@example.com",
            password = "password",
            role = Role.USER
        )

        // When
        val tokenPair = jwtService.generateToken(user)

        // Then
        assertNotNull(tokenPair.first)
        assertNotNull(tokenPair.second)

        val verifier = JWT.require(Algorithm.HMAC256(config.secret))
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .build()

        val accessDecodedJWT = verifier.verify(tokenPair.first)
        val refreshDecodedJWT = verifier.verify(tokenPair.second)
        val now = System.currentTimeMillis()

        assertEquals(config.issuer, accessDecodedJWT.issuer)
        assertEquals(config.audience, accessDecodedJWT.audience[0])
        assertEquals("test@example.com", accessDecodedJWT.getClaim("email").asString())
        assertEquals(user.role.name, accessDecodedJWT.getClaim("role").asString())
        assertTrue(accessDecodedJWT.expiresAt.after(Date(now)))
        assertTrue(accessDecodedJWT.expiresAt.before(Date(now + ACCESS_TOKEN_EXPIRES + 5000)))

        assertEquals(config.issuer, refreshDecodedJWT.issuer)
        assertEquals(config.audience, refreshDecodedJWT.audience[0])
        assertEquals("test@example.com", refreshDecodedJWT.getClaim("email").asString())
        assertEquals(user.role.name, refreshDecodedJWT.getClaim("role").asString())
        assertTrue(refreshDecodedJWT.expiresAt.after(Date(now)))
        assertTrue(refreshDecodedJWT.expiresAt.before(Date(now + REFRESH_TOKEN_EXPIRES + 5000)))
    }

    @Test
    fun `generateToken should tag access and refresh tokens with a type claim`() {
        val user = UserDTO(id = 1, name = "Test User", email = "test@example.com", password = "password", role = Role.USER)
        val tokenPair = jwtService.generateToken(user)

        val decodedAccess = JWT.decode(tokenPair.first)
        val decodedRefresh = JWT.decode(tokenPair.second)

        assertEquals("access", decodedAccess.getClaim("type").asString())
        assertEquals("refresh", decodedRefresh.getClaim("type").asString())
    }

    @Test
    fun `verify should return the decoded token for a valid token`() {
        val user = UserDTO(id = 1, name = "Test User", email = "test@example.com", password = "password", role = Role.USER)
        val tokenPair = jwtService.generateToken(user)

        val decoded = jwtService.verify(tokenPair.first)

        assertNotNull(decoded)
        assertEquals("test@example.com", decoded.getClaim("email").asString())
    }

    @Test
    fun `verify should return null for an expired token`() {
        val expiredToken = JWT.create()
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .withClaim("email", "test@example.com")
            .withClaim("type", "access")
            .withExpiresAt(Date(System.currentTimeMillis() - 1000))
            .sign(Algorithm.HMAC256(config.secret))

        assertNull(jwtService.verify(expiredToken))
    }

    @Test
    fun `verify should return null for a token signed with a different secret`() {
        val tamperedToken = JWT.create()
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .withClaim("email", "test@example.com")
            .withClaim("type", "access")
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000))
            .sign(Algorithm.HMAC256("wrong-secret"))

        assertNull(jwtService.verify(tamperedToken))
    }

    @Test
    fun `refreshAccessToken should mint a new access token from a valid refresh token`() {
        val user = UserDTO(id = 42, name = "Test User", email = "test@example.com", password = "password", role = Role.ADMIN)
        val tokenPair = jwtService.generateToken(user)

        val newAccessToken = jwtService.refreshAccessToken(tokenPair.second)

        assertNotNull(newAccessToken)
        val decoded = JWT.decode(newAccessToken)
        assertEquals("access", decoded.getClaim("type").asString())
        assertEquals("test@example.com", decoded.getClaim("email").asString())
        assertEquals("ADMIN", decoded.getClaim("role").asString())
        assertEquals(42L, decoded.getClaim("userId").asLong())
    }

    @Test
    fun `refreshAccessToken should return null when given an access token instead of a refresh token`() {
        val user = UserDTO(id = 1, name = "Test User", email = "test@example.com", password = "password", role = Role.USER)
        val tokenPair = jwtService.generateToken(user)

        assertNull(jwtService.refreshAccessToken(tokenPair.first))
    }

    @Test
    fun `refreshAccessToken should return null for an expired refresh token`() {
        val expiredRefreshToken = JWT.create()
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .withClaim("email", "test@example.com")
            .withClaim("role", "USER")
            .withClaim("userId", 1L)
            .withClaim("type", "refresh")
            .withExpiresAt(Date(System.currentTimeMillis() - 1000))
            .sign(Algorithm.HMAC256(config.secret))

        assertNull(jwtService.refreshAccessToken(expiredRefreshToken))
    }
}
