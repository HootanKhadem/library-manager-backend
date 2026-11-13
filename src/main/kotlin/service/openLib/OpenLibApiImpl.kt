package com.dw.service.openLib

import com.dw.model.dto.openlib.OpenLibSearchDoc
import com.dw.model.dto.openlib.OpenLibSearchResponse
import com.google.gson.Gson
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

class OpenLibApiImpl(
    private val responseFetcher: (String) -> String = ::fetchResponse,
    private val gson: Gson = Gson()
) : OpenLibApi {
    override fun getBookInfo(isbn: String): OpenLibSearchDoc {
        require(isbn.isNotBlank()) { "ISBN must not be blank" }

        val normalizedIsbn = isbn.trim()
        val encodedIsbn = URLEncoder.encode(normalizedIsbn, "UTF-8")
        val url = "$SEARCH_URL?isbn=$encodedIsbn&fields=$FIELDS"
        log.info("Fetching OpenLibrary search result: {}", url)
        val response = try {
            responseFetcher(url)
        } catch (e: Exception) {
            log.warn("OpenLibrary request failed for {}: {}", url, e.message)
            throw e
        }

        val result = gson.fromJson(response, OpenLibSearchResponse::class.java)
        return result.docs.firstOrNull()
            ?: throw NoSuchElementException("No OpenLibrary match for ISBN $normalizedIsbn")
    }

    private companion object {
        val log = LoggerFactory.getLogger(OpenLibApiImpl::class.java)

        // The edition-specific /isbn/{isbn}.json endpoint 404s for many real ISBNs that
        // aren't indexed under that exact edition record. The search endpoint resolves
        // ISBNs reliably and also returns the author's name directly, with no extra call.
        const val SEARCH_URL = "https://openlibrary.org/search.json"
        const val FIELDS = "title,author_name,first_publish_year,publisher,number_of_pages_median"

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
