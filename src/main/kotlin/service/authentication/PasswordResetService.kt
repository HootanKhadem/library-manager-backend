package com.dw.service.authentication

import com.dw.UserNotFoundException
import com.dw.db.PasswordResetTokenRepository
import com.dw.db.UserRepository
import com.dw.service.email.EmailMessage
import com.dw.service.email.EmailSender
import com.dw.service.util.PasswordUtil
import com.dw.service.util.PasswordUtil.Companion.sha256
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.time.Duration
import java.time.LocalDateTime
import java.util.Base64

interface PasswordResetServiceInterface {
    /** Never reveals whether [email] has an account: unknown emails and send failures are silent. */
    suspend fun requestReset(email: String)

    /** @throws IllegalArgumentException for an invalid/expired token or weak password. */
    suspend fun resetPassword(token: String, newPassword: String)
}

class PasswordResetService(
    private val userRepository: UserRepository,
    private val tokenRepository: PasswordResetTokenRepository,
    private val emailSender: EmailSender,
    private val frontendUrl: String,
    private val tokenTtl: Duration = Duration.ofHours(1),
    private val now: () -> LocalDateTime = { LocalDateTime.now() }
) : PasswordResetServiceInterface {

    private val log = LoggerFactory.getLogger(PasswordResetService::class.java)
    private val random = SecureRandom()

    override suspend fun requestReset(email: String) {
        val user = userRepository.findByEmail(email) ?: return
        val userId = user.id ?: return

        val bytes = ByteArray(32).also(random::nextBytes)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

        // One live token per user: a new request invalidates the previous link.
        tokenRepository.deleteByUserId(userId)
        tokenRepository.save(userId, token.sha256(), now().plus(tokenTtl))

        val link = "${frontendUrl.trimEnd('/')}/reset-password?token=$token"
        val minutes = tokenTtl.toMinutes()
        try {
            emailSender.send(
                EmailMessage(
                    to = user.email,
                    subject = "Reset your password",
                    html = "<p>Hi ${escape(user.name)},</p>" +
                        "<p><a href=\"$link\">Reset your password</a></p>" +
                        "<p>This link expires in $minutes minutes. If you did not request it, ignore this email.</p>",
                    text = "Reset your password: $link\nThis link expires in $minutes minutes."
                )
            )
        } catch (e: Exception) {
            log.error("Failed to send password reset email", e)
        }
    }

    override suspend fun resetPassword(token: String, newPassword: String) {
        PasswordUtil.requireStrong(newPassword)

        val stored = tokenRepository.findByHash(token.sha256())
        require(stored != null && stored.expiresOn.isAfter(now())) { "invalid or expired reset token" }

        val user = userRepository.findById(stored.userId) ?: throw UserNotFoundException("User not found")
        val salt = PasswordUtil.generateSalt()
        userRepository.update(
            user.copy(
                password = PasswordUtil.hashWithSalt(newPassword, salt),
                salt = salt,
                modifiedOn = now().toString()
            )
        )
        tokenRepository.deleteByUserId(stored.userId)
    }

    private fun escape(s: String) =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
