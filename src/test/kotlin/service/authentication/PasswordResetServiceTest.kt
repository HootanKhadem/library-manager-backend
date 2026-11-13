package service.authentication

import com.dw.db.mapping.UserDAO
import com.dw.db.postgres.passwordreset.PSQLPasswordResetTokenRepository
import com.dw.db.postgres.user.PSQLUserRepository
import com.dw.model.dto.Role
import com.dw.plugins.configureDatabases
import com.dw.service.authentication.PasswordResetService
import com.dw.service.email.EmailMessage
import com.dw.service.email.EmailSendException
import com.dw.service.email.EmailSender
import com.dw.service.util.PasswordUtil
import io.ktor.server.config.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Duration
import java.time.LocalDateTime
import kotlin.test.*

class PasswordResetServiceTest {

    private class FakeEmailSender(var fail: Boolean = false) : EmailSender {
        val sent = mutableListOf<EmailMessage>()
        override suspend fun send(message: EmailMessage) {
            if (fail) throw EmailSendException("boom")
            sent += message
        }
    }

    private val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:reset_service_test;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver"
    )

    private val userRepository = PSQLUserRepository()
    private val tokenRepository = PSQLPasswordResetTokenRepository()
    private val sender = FakeEmailSender()
    private var clock = LocalDateTime.of(2030, 1, 1, 12, 0)

    private val service = PasswordResetService(
        userRepository = userRepository,
        tokenRepository = tokenRepository,
        emailSender = sender,
        frontendUrl = "https://app.example.com/",
        tokenTtl = Duration.ofMinutes(60),
        now = { clock }
    )

    @BeforeTest
    fun setup() {
        configureDatabases(testConfig)
        val salt = PasswordUtil.generateSalt()
        transaction {
            UserDAO.new {
                username = "alice"
                email = "alice@example.com"
                password = PasswordUtil.hashWithSalt("OldPassword1", salt)
                role = Role.USER.name
                this.salt = salt
            }
        }
    }

    @AfterTest
    fun tearDown() {
        transaction { exec("DROP ALL OBJECTS") }
    }

    private fun tokenFromEmail(): String =
        Regex("token=([A-Za-z0-9_-]+)").find(sender.sent.last().text!!)!!.groupValues[1]

    @Test
    fun `requestReset emails a frontend link to a registered user`(): Unit = runBlocking {
        service.requestReset("alice@example.com")

        val mail = sender.sent.single()
        assertEquals("alice@example.com", mail.to)
        assertTrue(mail.text!!.contains("https://app.example.com/reset-password?token="))
        assertTrue(mail.html.contains("https://app.example.com/reset-password?token="))
    }

    @Test
    fun `requestReset stores only a hash, never the raw token`(): Unit = runBlocking {
        service.requestReset("alice@example.com")
        val token = tokenFromEmail()

        assertNull(tokenRepository.findByHash(token))
        assertNotNull(tokenRepository.findByHash(with(PasswordUtil) { token.sha256() }))
    }

    @Test
    fun `requestReset for unknown email sends nothing and does not throw`(): Unit = runBlocking {
        service.requestReset("ghost@example.com")
        assertTrue(sender.sent.isEmpty())
    }

    @Test
    fun `requestReset swallows email send failures`(): Unit = runBlocking {
        sender.fail = true
        service.requestReset("alice@example.com")
    }

    @Test
    fun `resetPassword changes the password and consumes the token`(): Unit = runBlocking {
        service.requestReset("alice@example.com")
        val token = tokenFromEmail()

        service.resetPassword(token, "NewPassword1")

        val user = userRepository.findByEmail("alice@example.com")!!
        assertTrue(PasswordUtil.verify("NewPassword1", user.salt!!, user.password))
        assertFalse(PasswordUtil.verify("OldPassword1", user.salt!!, user.password))
        assertFailsWith<IllegalArgumentException> { service.resetPassword(token, "AnotherPass1") }
    }

    @Test
    fun `second request invalidates the first token`(): Unit = runBlocking {
        service.requestReset("alice@example.com")
        val first = tokenFromEmail()
        service.requestReset("alice@example.com")

        assertFailsWith<IllegalArgumentException> { service.resetPassword(first, "NewPassword1") }
        service.resetPassword(tokenFromEmail(), "NewPassword1")
    }

    @Test
    fun `expired token is rejected`(): Unit = runBlocking {
        service.requestReset("alice@example.com")
        val token = tokenFromEmail()
        clock = clock.plusMinutes(61)

        assertFailsWith<IllegalArgumentException> { service.resetPassword(token, "NewPassword1") }
    }

    @Test
    fun `unknown token is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            runBlocking { service.resetPassword("bogus", "NewPassword1") }
        }
    }

    @Test
    fun `weak password is rejected and token stays usable`(): Unit = runBlocking {
        service.requestReset("alice@example.com")
        val token = tokenFromEmail()

        assertFailsWith<IllegalArgumentException> { service.resetPassword(token, "weak") }
        service.resetPassword(token, "NewPassword1")
    }
}
