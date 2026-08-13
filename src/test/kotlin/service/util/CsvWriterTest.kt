package service.util

import com.dw.service.util.CsvWriter
import kotlin.test.Test
import kotlin.test.assertEquals

class CsvWriterTest {

    @Test
    fun `writes header row and data rows`() {
        val csv = CsvWriter.write(
            headers = listOf("id", "name"),
            rows = listOf(listOf("1", "Fiction"), listOf("2", "History"))
        )
        assertEquals("id,name\r\n1,Fiction\r\n2,History\r\n", csv)
    }

    @Test
    fun `quotes fields containing commas`() {
        val csv = CsvWriter.write(
            headers = listOf("name"),
            rows = listOf(listOf("Smith, John"))
        )
        assertEquals("name\r\n\"Smith, John\"\r\n", csv)
    }

    @Test
    fun `escapes embedded quotes by doubling them`() {
        val csv = CsvWriter.write(
            headers = listOf("motto"),
            rows = listOf(listOf("""She said "hello"."""))
        )
        assertEquals("motto\r\n\"She said \"\"hello\"\".\"\r\n", csv)
    }

    @Test
    fun `quotes fields containing newlines`() {
        val csv = CsvWriter.write(
            headers = listOf("description"),
            rows = listOf(listOf("line1\nline2"))
        )
        assertEquals("description\r\n\"line1\nline2\"\r\n", csv)
    }

    @Test
    fun `renders null fields as empty string`() {
        val csv = CsvWriter.write(
            headers = listOf("a", "b"),
            rows = listOf(listOf("x", null))
        )
        assertEquals("a,b\r\nx,\r\n", csv)
    }

    @Test
    fun `writes header only when there are no rows`() {
        val csv = CsvWriter.write(headers = listOf("a", "b"), rows = emptyList())
        assertEquals("a,b\r\n", csv)
    }
}
