package db.postgres.book

import com.dw.db.mapping.AuthorDAO
import com.dw.db.mapping.AuthorTable
import com.dw.db.mapping.BookDAO
import com.dw.db.mapping.BookTable
import com.dw.db.mapping.UserTable
import com.dw.db.postgres.book.PSQLBookRepository
import com.dw.plugins.configureDatabases
import io.ktor.server.config.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
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
        transaction {
            SchemaUtils.drop(BookTable, AuthorTable, UserTable)
        }
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
}
