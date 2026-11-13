package com.dw.service.openLib

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OpenLibApiImplTest {
    @Test
    fun `getBookInfo requests the search endpoint and maps the first match`() {
        val responseFetcher = MockResponseFetcher(
            response = """
                {
                  "numFound": 1,
                  "docs": [
                    {
                      "title": "American Gods",
                      "author_name": ["Neil Gaiman"],
                      "first_publish_year": 2001,
                      "publisher": ["William Morrow", "HarperCollins"],
                      "number_of_pages_median": 576
                    }
                  ]
                }
            """
        )
        val api = OpenLibApiImpl(responseFetcher = responseFetcher)

        val doc = api.getBookInfo(" 9781472249081 ")

        assertEquals(1, responseFetcher.callCount)
        assertEquals(
            "https://openlibrary.org/search.json?isbn=9781472249081" +
                "&fields=title,author_name,first_publish_year,publisher,number_of_pages_median",
            responseFetcher.requestedUrl
        )
        assertEquals("American Gods", doc.title)
        assertEquals(listOf("Neil Gaiman"), doc.authorName)
        assertEquals(2001, doc.firstPublishYear)
        assertEquals(listOf("William Morrow", "HarperCollins"), doc.publisher)
        assertEquals(576, doc.numberOfPagesMedian)
    }

    @Test
    fun `getBookInfo throws when the search returns no matches`() {
        val responseFetcher = MockResponseFetcher(response = """{ "numFound": 0, "docs": [] }""")
        val api = OpenLibApiImpl(responseFetcher = responseFetcher)

        assertFailsWith<NoSuchElementException> { api.getBookInfo("0000000000") }
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
