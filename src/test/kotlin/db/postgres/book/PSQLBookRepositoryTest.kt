package db.postgres.book

import com.dw.db.mapping.AuthorDAO
import com.dw.db.mapping.BookDAO
import com.dw.db.postgres.book.PSQLBookRepository
import com.dw.plugins.configureDatabases
import io.ktor.server.config.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*

class PSQLBookRepositoryTest {

    private val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:repo_book_test;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver"
    )

    private val repository = PSQLBookRepository()

    @BeforeTest
    fun setup() {
        configureDatabases(testConfig)
    }

    @AfterTest
    fun tearDown() {
        transaction { exec("DROP ALL OBJECTS") }
    }

    private fun createAuthorAndBook(
        isbn: String,
        userId: Long,
        name: String = "Book $isbn",
        authorName: String = "Author for $isbn",
        status: String = "OWNED",
    ) {
        transaction {
            val authorDAO = AuthorDAO.new {
                this.name = authorName
                image = "image.jpg"
            }
            BookDAO.new {
                this.name = name
                author = authorDAO
                this.isbn = isbn
                pages = 100
                publishedDate = "2020-01-01"
                publisher = "Publisher"
                quantity = 1
                this.userId = userId
                this.status = status
            }
        }
    }

    @Test
    fun `countByUserId returns correct count`() = runBlocking {
        createAuthorAndBook("isbn1", 1L)
        createAuthorAndBook("isbn2", 1L)
        createAuthorAndBook("isbn3", 2L)

        assertEquals(2, repository.countByUserId(1L))
        assertEquals(1, repository.countByUserId(2L))
        assertEquals(0, repository.countByUserId(3L))
    }

    @Test
    fun `findAllByUserIdPaged returns first page ordered by id`() = runBlocking {
        createAuthorAndBook("isbn-p1", 5L)
        createAuthorAndBook("isbn-p2", 5L)
        createAuthorAndBook("isbn-p3", 5L)

        val page1 = repository.findAllByUserIdPaged(5L, page = 1, pageSize = 2)
        assertEquals(2, page1.size)
        assertEquals("Book isbn-p1", page1[0].name)
        assertEquals("Book isbn-p2", page1[1].name)
    }

    @Test
    fun `findAllByUserIdPaged returns second page`() = runBlocking {
        createAuthorAndBook("isbn-p1", 5L)
        createAuthorAndBook("isbn-p2", 5L)
        createAuthorAndBook("isbn-p3", 5L)

        val page2 = repository.findAllByUserIdPaged(5L, page = 2, pageSize = 2)
        assertEquals(1, page2.size)
        assertEquals("Book isbn-p3", page2[0].name)
    }

    @Test
    fun `findAllByUserIdPaged returns empty list beyond last page`() = runBlocking {
        createAuthorAndBook("isbn-p1", 5L)

        val page3 = repository.findAllByUserIdPaged(5L, page = 3, pageSize = 2)
        assertEquals(0, page3.size)
    }

    @Test
    fun `findAllByUserIdPaged filters by userId`() = runBlocking {
        createAuthorAndBook("isbn-u1", 1L)
        createAuthorAndBook("isbn-u2", 2L)

        val page1 = repository.findAllByUserIdPaged(1L, page = 1, pageSize = 10)
        assertEquals(1, page1.size)
        assertEquals("Book isbn-u1", page1[0].name)
    }

    @Test
    fun `findAllByUserIdPaged filters by search matching book name`() = runBlocking {
        createAuthorAndBook("isbn-dune", 9L, name = "Dune")
        createAuthorAndBook("isbn-ficciones", 9L, name = "Ficciones")

        val results = repository.findAllByUserIdPaged(9L, page = 1, pageSize = 10, search = "dun")
        assertEquals(1, results.size)
        assertEquals("Dune", results[0].name)
    }

    @Test
    fun `findAllByUserIdPaged filters by search matching isbn`() = runBlocking {
        createAuthorAndBook("isbn-match-123", 9L)
        createAuthorAndBook("isbn-other-456", 9L)

        val results = repository.findAllByUserIdPaged(9L, page = 1, pageSize = 10, search = "match-123")
        assertEquals(1, results.size)
        assertEquals("isbn-match-123", results[0].isbn)
    }

    @Test
    fun `findAllByUserIdPaged filters by search matching author name`() = runBlocking {
        createAuthorAndBook("isbn-a1", 9L, authorName = "Frank Herbert")
        createAuthorAndBook("isbn-a2", 9L, authorName = "Jorge Luis Borges")

        val results = repository.findAllByUserIdPaged(9L, page = 1, pageSize = 10, search = "herbert")
        assertEquals(1, results.size)
        assertEquals("isbn-a1", results[0].isbn)
    }

    @Test
    fun `findAllByUserIdPaged search is case-insensitive`() = runBlocking {
        createAuthorAndBook("isbn-case", 9L, name = "Dune")

        val results = repository.findAllByUserIdPaged(9L, page = 1, pageSize = 10, search = "DUNE")
        assertEquals(1, results.size)
    }

    @Test
    fun `findAllByUserIdPaged treats blank search as no filter`() = runBlocking {
        createAuthorAndBook("isbn-b1", 9L)
        createAuthorAndBook("isbn-b2", 9L)

        val results = repository.findAllByUserIdPaged(9L, page = 1, pageSize = 10, search = "   ")
        assertEquals(2, results.size)
    }

    @Test
    fun `findAllByUserIdPaged escapes literal percent and underscore in search`() = runBlocking {
        createAuthorAndBook("isbn-percent", 9L, name = "50% Off")
        createAuthorAndBook("isbn-decoy", 9L, name = "50X Off")

        val results = repository.findAllByUserIdPaged(9L, page = 1, pageSize = 10, search = "50%")
        assertEquals(1, results.size)
        assertEquals("50% Off", results[0].name)
    }

    @Test
    fun `findAllByUserIdPaged filters by status`() = runBlocking {
        createAuthorAndBook("isbn-owned", 9L, status = "OWNED")
        createAuthorAndBook("isbn-lent", 9L, status = "LENT_OUT")

        val results = repository.findAllByUserIdPaged(9L, page = 1, pageSize = 10, status = "LENT_OUT")
        assertEquals(1, results.size)
        assertEquals("isbn-lent", results[0].isbn)
    }

    @Test
    fun `findAllByUserIdPaged combines search and status`() = runBlocking {
        createAuthorAndBook("isbn-dune-owned", 9L, name = "Dune", status = "OWNED")
        createAuthorAndBook("isbn-dune-lent", 9L, name = "Dune", status = "LENT_OUT")

        val results = repository.findAllByUserIdPaged(9L, page = 1, pageSize = 10, search = "dune", status = "OWNED")
        assertEquals(1, results.size)
        assertEquals("isbn-dune-owned", results[0].isbn)
    }

    @Test
    fun `findAllByUserIdPaged search never returns another user's books`() = runBlocking {
        createAuthorAndBook("isbn-mine", 9L, name = "Shared Title")
        createAuthorAndBook("isbn-theirs", 10L, name = "Shared Title")

        val results = repository.findAllByUserIdPaged(9L, page = 1, pageSize = 10, search = "shared")
        assertEquals(1, results.size)
        assertEquals("isbn-mine", results[0].isbn)
    }

    @Test
    fun `countByUserId reflects the filtered search count, not the whole library`() = runBlocking {
        createAuthorAndBook("isbn-match-1", 9L, name = "Dune")
        createAuthorAndBook("isbn-match-2", 9L, name = "Dune Messiah")
        createAuthorAndBook("isbn-nomatch", 9L, name = "Ficciones")

        assertEquals(2, repository.countByUserId(9L, search = "dune"))
        assertEquals(3, repository.countByUserId(9L))
    }
}
