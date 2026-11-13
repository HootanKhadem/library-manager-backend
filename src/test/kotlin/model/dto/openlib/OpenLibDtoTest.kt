package com.dw.model.dto.openlib

import com.google.gson.Gson
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenLibDtoTest {
    @Test
    fun `Gson maps Open Library response to camel case fields`() {
        val dto = Gson().fromJson(response, OpenLibDto::class.java)

        assertEquals(listOf("goodreads-123", "librarything-456"), dto.identifiers?.goodreads)
        assertEquals(listOf("lt-789"), dto.identifiers?.librarything)
        assertEquals("Fantastic Mr. Fox", dto.title)
        assertEquals("October 1, 1988", dto.publishDate)
        assertEquals("/authors/OL1A", dto.authors.single().key)
        assertEquals(listOf("Faber & Faber"), dto.publishers)
        assertEquals(listOf(1, 2, 3), dto.covers)
        assertEquals(listOf("Roald Dahl"), dto.contributions)
        assertEquals("/languages/eng", dto.languages.single().key)
        assertEquals(listOf("marc:123"), dto.sourceRecords)
        assertEquals(listOf("id-123"), dto.localId)
        assertEquals("/type/edition", dto.type?.key)
        assertEquals("/type/text", dto.firstSentence?.type)
        assertEquals("A story.", dto.firstSentence?.value)
        assertEquals("/books/OL7353617M", dto.key)
        assertEquals(96, dto.numberOfPages)
        assertEquals("/works/OL45804W", dto.works.single().key)
        assertEquals(listOf("PS"), dto.classifications["lc_classifications"])
        assertEquals("fantasticmrf...", dto.ocaid)
        assertEquals(listOf("0140328726"), dto.isbn10)
        assertEquals(listOf("9780140328721"), dto.isbn13)
        assertEquals(28, dto.latestRevision)
        assertEquals(28, dto.revision)
        assertEquals("/type/datetime", dto.created?.type)
        assertEquals("2020-01-01T00:00:00.000000", dto.created?.value)
        assertEquals("/type/datetime", dto.lastModified?.type)
        assertEquals("2024-01-01T00:00:00.000000", dto.lastModified?.value)
    }

    private companion object {
        const val response = """
            {
              "identifiers": {
                "goodreads": ["goodreads-123", "librarything-456"],
                "librarything": ["lt-789"]
              },
              "title": "Fantastic Mr. Fox",
              "authors": [{"key": "/authors/OL1A"}],
              "publish_date": "October 1, 1988",
              "publishers": ["Faber & Faber"],
              "covers": [1, 2, 3],
              "contributions": ["Roald Dahl"],
              "languages": [{"key": "/languages/eng"}],
              "source_records": ["marc:123"],
              "local_id": ["id-123"],
              "type": {"key": "/type/edition"},
              "first_sentence": {"type": "/type/text", "value": "A story."},
              "key": "/books/OL7353617M",
              "number_of_pages": 96,
              "works": [{"key": "/works/OL45804W"}],
              "classifications": {"lc_classifications": ["PS"]},
              "ocaid": "fantasticmrf...",
              "isbn_10": ["0140328726"],
              "isbn_13": ["9780140328721"],
              "latest_revision": 28,
              "revision": 28,
              "created": {"type": "/type/datetime", "value": "2020-01-01T00:00:00.000000"},
              "last_modified": {"type": "/type/datetime", "value": "2024-01-01T00:00:00.000000"}
            }
        """
    }
}
