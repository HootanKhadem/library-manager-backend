package service.email

import com.dw.service.email.EmailMessage
import com.dw.service.email.EmailSendException
import com.dw.service.email.ResendEmailSender
import com.google.gson.Gson
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import kotlin.test.*

class ResendEmailSenderTest {

    private lateinit var server: HttpServer
    private var status = 200
    private var lastAuth: String? = null
    private var lastBody: String? = null
    private var lastMethod: String? = null
    private var lastPath: String? = null

    @BeforeTest
    fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { ex ->
            lastAuth = ex.requestHeaders.getFirst("Authorization")
            lastMethod = ex.requestMethod
            lastPath = ex.requestURI.path
            lastBody = ex.requestBody.readBytes().decodeToString()
            val resp = if (status in 200..299) """{"id":"abc"}""" else """{"message":"bad"}"""
            ex.sendResponseHeaders(status, resp.length.toLong())
            ex.responseBody.use { it.write(resp.toByteArray()) }
        }
        server.start()
    }

    @AfterTest
    fun stop() = server.stop(0)

    private fun sender(key: String = "re_test") =
        ResendEmailSender(key, "App <noreply@example.com>", "http://127.0.0.1:${server.address.port}/emails")

    @Test
    fun `posts JSON with bearer key to the configured endpoint`(): Unit = runBlocking {
        sender().send(EmailMessage("u@example.com", "Hi", "<b>x</b>", "x"))

        assertEquals("POST", lastMethod)
        assertEquals("/emails", lastPath)
        assertEquals("Bearer re_test", lastAuth)
        val body = Gson().fromJson(lastBody, Map::class.java)
        assertEquals("App <noreply@example.com>", body["from"])
        assertEquals(listOf("u@example.com"), body["to"])
        assertEquals("Hi", body["subject"])
        assertEquals("<b>x</b>", body["html"])
        assertEquals("x", body["text"])
    }

    @Test
    fun `omits text when not provided`(): Unit = runBlocking {
        sender().send(EmailMessage("u@example.com", "Hi", "<b>x</b>"))
        assertFalse(Gson().fromJson(lastBody, Map::class.java).containsKey("text"))
    }

    @Test
    fun `non-2xx response throws EmailSendException with status`() {
        status = 422
        val e = assertFailsWith<EmailSendException> {
            runBlocking { sender().send(EmailMessage("u@example.com", "Hi", "x")) }
        }
        assertTrue(e.message!!.contains("422"))
    }

    @Test
    fun `blank api key throws without calling the server`() {
        assertFailsWith<EmailSendException> {
            runBlocking { sender(key = "").send(EmailMessage("u@example.com", "Hi", "x")) }
        }
        assertNull(lastMethod)
    }

    @Test
    fun `unreachable server throws EmailSendException`() {
        val dead = ResendEmailSender("k", "a@b.c", "http://127.0.0.1:1/emails")
        assertFailsWith<EmailSendException> {
            runBlocking { dead.send(EmailMessage("u@example.com", "Hi", "x")) }
        }
    }
}
