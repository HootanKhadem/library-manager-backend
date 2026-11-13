package com.dw.service.openLib

import com.dw.model.dto.openlib.OpenLibAuthorDto
import com.dw.model.dto.openlib.OpenLibDto
import com.google.gson.Gson
import java.net.HttpURLConnection
import java.net.URI

class OpenLibApiImpl(
    private val responseFetcher: (String) -> String = ::fetchResponse,
    private val gson: Gson = Gson()
) : OpenLibApi {
    override fun getBookInfo(isbn: String): OpenLibDto {
        require(isbn.isNotBlank()) { "ISBN must not be blank" }

        val normalizedIsbn = isbn.trim()
        val response = responseFetcher("$BASE_URL$normalizedIsbn.json")
        return gson.fromJson(response, OpenLibDto::class.java)
    }

    override fun getAuthorName(authorKey: String): String? {
        require(authorKey.isNotBlank()) { "Author key must not be blank" }

        val response = responseFetcher("$ROOT_URL${authorKey.trim()}.json")
        return gson.fromJson(response, OpenLibAuthorDto::class.java).name
    }

    private companion object {
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
