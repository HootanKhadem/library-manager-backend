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

    private fun createAuthorAndBook(isbn: String, userId: Long) {
        transaction {
            val authorDAO = AuthorDAO.new {
                name = "Author for $isbn"
                image = "image.jpg"
            }
            BookDAO.new {
                name = "Book $isbn"
                author = authorDAO
                this.isbn = isbn
                pages = 100
                publishedDate = "2020-01-01"
                publisher = "Publisher"
                quantity = 1
                this.userId = userId
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
}
