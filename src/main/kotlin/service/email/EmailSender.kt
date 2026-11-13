package com.dw.service.email

data class EmailMessage(
    val to: String,
    val subject: String,
    val html: String,
    val text: String? = null
)

class EmailSendException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/** Provider-agnostic outbound email. Throws [EmailSendException] when delivery is not accepted. */
interface EmailSender {
    suspend fun send(message: EmailMessage)
}
