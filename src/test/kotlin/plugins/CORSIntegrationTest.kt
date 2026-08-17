package com.dw.plugins

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CORSIntegrationTest {

    private fun testCORSApplication(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        environment {
            config = MapApplicationConfig(
                "ktor.cors.allowedOrigin" to "http://localhost:3000"
            )
        }
        application {
            configureHTTP()
            routing {
                get("/ping") { call.respondText("pong") }
            }
        }
        block()
    }

    @Test
    fun `preflight from the allowed origin is accepted with credentials`() = testCORSApplication {
        val response = client.options("/ping") {
            header(HttpHeaders.Origin, "http://localhost:3000")
            header(HttpHeaders.AccessControlRequestMethod, "GET")
        }

        assertEquals("http://localhost:3000", response.headers[HttpHeaders.AccessControlAllowOrigin])
        assertEquals("true", response.headers[HttpHeaders.AccessControlAllowCredentials])
    }

    @Test
    fun `request from a different origin is not granted CORS headers`() = testCORSApplication {
        val response = client.get("/ping") {
            header(HttpHeaders.Origin, "http://evil.example.com")
        }

        assertNull(response.headers[HttpHeaders.AccessControlAllowOrigin])
    }
}
