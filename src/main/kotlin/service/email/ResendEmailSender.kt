package com.dw.service.email

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** Resend implementation: https://resend.com/docs/api-reference/emails/send-email */
class ResendEmailSender(
    private val apiKey: String,
    private val from: String,
    private val endpoint: String = DEFAULT_ENDPOINT,
    private val client: HttpClient = HttpClient.newHttpClient(),
    private val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
) : EmailSender {

    override suspend fun send(message: EmailMessage) {
        if (apiKey.isBlank()) throw EmailSendException("RESEND_API_KEY is not configured")

        val body = gson.toJson(
            mapOf(
                "from" to from,
                "to" to listOf(message.to),
                "subject" to message.subject,
                "html" to message.html,
                "text" to message.text
            )
        )
        val request = HttpRequest.newBuilder(URI.create(endpoint))
            .timeout(Duration.ofSeconds(10))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = try {
            withContext(Dispatchers.IO) { client.send(request, HttpResponse.BodyHandlers.ofString()) }
        } catch (e: IOException) {
            throw EmailSendException("Resend request failed: ${e.message}", e)
        }
        if (response.statusCode() !in 200..299) {
            throw EmailSendException("Resend rejected email with HTTP ${response.statusCode()}: ${response.body()}")
        }
    }

    companion object {
        const val DEFAULT_ENDPOINT = "https://api.resend.com/emails"
    }
}
