package routes.helloWorld

import com.dw.plugins.configureDependencyInjection
import com.dw.plugins.configureJWT
import com.dw.plugins.configurePublicRouting
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class HelloWorldTest {
    @Test
    fun testRoot() = testApplication {
        environment {
            config = MapApplicationConfig(
                "ktor.jwt.secret" to "secret",
                "ktor.jwt.issuer" to "issuer",
                "ktor.jwt.audience" to "audience",
                "ktor.jwt.realm" to "realm"
            )
        }
        application {
            configureDependencyInjection()
            configureJWT()
            configurePublicRouting()
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Hello World!", response.bodyAsText())
    }
}