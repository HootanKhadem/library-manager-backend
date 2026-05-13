package routes.helloWorld

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import routes.BaseRouteTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HelloWorldTest : BaseRouteTest() {
    @Test
    fun testRoot() = testApplication {
        setupLibraryApp()
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Hello World!", response.bodyAsText())
    }
}