package com.dw.service.authentication

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.dw.model.dto.Role
import com.dw.model.dto.UserDTO
import com.dw.plugins.JwtConfig
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
    fun `generateToken should return a valid JWT`() {
        // Given
        val user = UserDTO(
            id = 1,
            name = "Test User",
            email = "test@example.com",
            password = "password",
            role = Role.USER
        )

        // When
        val token = jwtService.generateToken(user)

        // Then
        assertNotNull(token)

        val verifier = JWT.require(Algorithm.HMAC256(config.secret))
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .build()

        val decodedJWT = verifier.verify(token)

        assertEquals(config.issuer, decodedJWT.issuer)
        assertEquals(config.audience, decodedJWT.audience[0])
        assertEquals("test@example.com", decodedJWT.getClaim("email").asString())
        assertEquals(user.role.name, decodedJWT.getClaim("role").asString())
        assertTrue(decodedJWT.expiresAt.after(Date()))
        assertTrue(decodedJWT.expiresAt.before(Date(System.currentTimeMillis() + 3600001)))
    }
}
