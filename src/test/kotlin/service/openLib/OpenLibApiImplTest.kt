package com.dw.service.openLib

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OpenLibApiImplTest {
    @Test
    fun `getBookInfo requests ISBN endpoint and maps response`() {
        val responseFetcher = MockResponseFetcher(
            response = """
                {
                  "title": "Fantastic Mr. Fox",
                  "publish_date": "October 1, 1988",
                  "number_of_pages": 96,
                  "isbn_10": ["0140328726"],
                  "last_modified": {
                    "type": "/type/datetime",
                    "value": "2024-01-01T00:00:00.000000"
                  }
                }
            """
        )
        val api = OpenLibApiImpl(responseFetcher = responseFetcher)

        val book = api.getBookInfo(" 0140328726 ")

        assertEquals(1, responseFetcher.callCount)
        assertEquals("https://openlibrary.org/isbn/0140328726.json", responseFetcher.requestedUrl)
        assertEquals("Fantastic Mr. Fox", book.title)
        assertEquals("October 1, 1988", book.publishDate)
        assertEquals(96, book.numberOfPages)
        assertEquals(listOf("0140328726"), book.isbn10)
        assertEquals("2024-01-01T00:00:00.000000", book.lastModified?.value)
    }

    @Test
    fun `getBookInfo rejects blank ISBN`() {
        val responseFetcher = MockResponseFetcher(failure = AssertionError("request must not happen"))
        val api = OpenLibApiImpl(responseFetcher = responseFetcher)

        assertFailsWith<IllegalArgumentException> { api.getBookInfo("   ") }
        assertEquals(0, responseFetcher.callCount)
    }

    private class MockResponseFetcher(
        private val response: String = "",
        private val failure: Throwable? = null
    ) : (String) -> String {
        var callCount = 0
            private set
        var requestedUrl: String? = null
            private set

        override fun invoke(url: String): String {
            callCount++
            requestedUrl = url
            failure?.let { throw it }
            return response
        }
    }
}
