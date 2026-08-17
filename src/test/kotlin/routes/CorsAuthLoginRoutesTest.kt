package routes

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CorsAuthLoginRoutesTest : BaseRouteTest() {

    @AfterTest
    fun tearDown() = cleanup()

    @Test
    fun `preflight for POST auth login from allowed origin allows content-type and credentials`() = testApplication {
        setupLibraryApp(includeCors = true)

        val response = client.options("/auth/login") {
            header(HttpHeaders.Origin, "http://localhost:3000")
            header(HttpHeaders.AccessControlRequestMethod, "POST")
            header(HttpHeaders.AccessControlRequestHeaders, "content-type")
        }

        // POST is a CORS-safelisted method, so Ktor's CORS plugin omits it from
        // Access-Control-Allow-Methods by design; a 2xx status is what proves the
        // preflight for POST was accepted rather than rejected.
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("http://localhost:3000", response.headers[HttpHeaders.AccessControlAllowOrigin])
        assertEquals("true", response.headers[HttpHeaders.AccessControlAllowCredentials])

        val allowedHeaders = response.headers[HttpHeaders.AccessControlAllowHeaders].orEmpty()
        assertTrue(
            allowedHeaders.contains("Content-Type", ignoreCase = true),
            "expected Content-Type in allowed headers, got: $allowedHeaders"
        )
    }
}
