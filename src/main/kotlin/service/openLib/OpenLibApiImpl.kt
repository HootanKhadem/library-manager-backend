package com.dw.service.openLib

import com.dw.model.dto.openlib.OpenLibAuthorDto
import com.dw.model.dto.openlib.OpenLibDto
import com.google.gson.Gson
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.URI

class OpenLibApiImpl(
    private val responseFetcher: (String) -> String = ::fetchResponse,
    private val gson: Gson = Gson()
) : OpenLibApi {
    override fun getBookInfo(isbn: String): OpenLibDto {
        require(isbn.isNotBlank()) { "ISBN must not be blank" }

        val normalizedIsbn = isbn.trim()
        val url = "$BASE_URL$normalizedIsbn.json"
        log.info("Fetching OpenLibrary book info: {}", url)
        val response = try {
            responseFetcher(url)
        } catch (e: Exception) {
            log.warn("OpenLibrary request failed for {}: {}", url, e.message)
            throw e
        }
        return gson.fromJson(response, OpenLibDto::class.java)
    }

    override fun getAuthorName(authorKey: String): String? {
        require(authorKey.isNotBlank()) { "Author key must not be blank" }

        val url = "$ROOT_URL${authorKey.trim()}.json"
        log.info("Fetching OpenLibrary author info: {}", url)
        val response = try {
            responseFetcher(url)
        } catch (e: Exception) {
            log.warn("OpenLibrary author request failed for {}: {}", url, e.message)
            throw e
        }
        return gson.fromJson(response, OpenLibAuthorDto::class.java).name
    }

    private companion object {
        val log = LoggerFactory.getLogger(OpenLibApiImpl::class.java)
        const val ROOT_URL = "https://openlibrary.org"
        const val BASE_URL = "https://openlibrary.org/isbn/"

        fun fetchResponse(url: String): String {
            val connection = URI.create(url).toURL().openConnection() as HttpURLConnection
            return try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000

                val statusCode = connection.responseCode
                if (statusCode !in 200..299) {
                    throw IllegalStateException("Open Library request failed with HTTP $statusCode")
                }

                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }
    }
}
