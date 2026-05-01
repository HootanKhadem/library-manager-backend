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
}
