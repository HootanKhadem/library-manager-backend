package db.mapping

import com.dw.db.mapping.AuthorDAO
import com.dw.db.mapping.AuthorTable
import com.dw.db.mapping.BookDAO
import com.dw.db.mapping.BookTable
import com.dw.db.mapping.UserTable
import com.dw.model.dto.Author
import com.dw.model.dto.Book
import com.dw.plugins.configureDatabases
import io.ktor.server.config.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*

class BookDAOTest {

    private val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:book_dao_test;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver"
    )

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

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun makeAuthor(name: String = "J.K. Rowling", image: String = "jk.jpg") =
        transaction {
            AuthorDAO.new {
                this.name = name
                this.image = image
            }
        }

    private fun makeBook(authorDAO: AuthorDAO, isbn: String = "isbn-001") =
        transaction {
            BookDAO.new {
                name = "Harry Potter"
                author = authorDAO
                translator = null
                pages = 300
                this.isbn = isbn
                publishedDate = "1997-06-26"
                publisher = "Bloomsbury"
                quantity = 10
                image = "hp.jpg"
            }
        }

    // ── create & read ─────────────────────────────────────────────────────────

    @Test
    fun testCreateAndReadBook() {
        val authorDAO = makeAuthor()
        val bookDAO = makeBook(authorDAO)

        transaction {
            val retrieved = BookDAO.findById(bookDAO.id)
            assertNotNull(retrieved)
            assertEquals("Harry Potter", retrieved.name)
            assertEquals("J.K. Rowling", retrieved.author.name)
        }
    }

    @Test
    fun testUpdateBook() {
        val authorDAO = makeAuthor(name = "Author")
        val bookDAO = makeBook(authorDAO, isbn = "isbn-002")

        transaction {
            val dao = BookDAO.findById(bookDAO.id)!!
            dao.name = "New Name"
            assertEquals("New Name", BookDAO.findById(bookDAO.id)?.name)
        }
    }

    // ── toBookDto ─────────────────────────────────────────────────────────────

    @Test
    fun testToDto() {
        val authorDAO = makeAuthor()
        val bookDAO = makeBook(authorDAO, isbn = "isbn-003")

        transaction {
            val dao = BookDAO.findById(bookDAO.id)!!
            val dto = dao.toBookDto()
            assertEquals(dao.id.value, dto.id)
            assertEquals(dao.name, dto.name)
            assertEquals(authorDAO.name, dto.author.name)
            assertEquals(dao.image, dto.image)
        }
    }

    // ── translator field ──────────────────────────────────────────────────────

    @Test
    fun testTranslatorField() {
        val authorDAO = makeAuthor()
        transaction {
            val bookDAO = BookDAO.new {
                name = "Translated Book"
                author = authorDAO
                translator = "Some Translator"
                pages = 200
                isbn = "isbn-trans"
                publishedDate = "2000-01-01"
                publisher = "Pub"
                quantity = 1
                image = null
            }
            val dto = bookDAO.toBookDto()
            assertEquals("Some Translator", dto.translator)
        }
    }

    @Test
    fun testTranslatorNullable() {
        val authorDAO = makeAuthor()
        transaction {
            val bookDAO = BookDAO.new {
                name = "No Translator"
                author = authorDAO
                translator = null
                pages = 100
                isbn = "isbn-no-trans"
                publishedDate = "2000-01-01"
                publisher = "Pub"
                quantity = 1
                image = null
            }
            assertNull(bookDAO.toBookDto().translator)
        }
    }

    // ── audit fields ──────────────────────────────────────────────────────────

    @Test
    fun testAuditFields() {
        val authorDAO = makeAuthor()
        transaction {
            val bookDAO = BookDAO.new {
                name = "Audited Book"
                author = authorDAO
                pages = 100
                isbn = "isbn-audit"
                publishedDate = "2000-01-01"
                publisher = "Pub"
                quantity = 1
                image = null
                createdOn = "2024-01-01T00:00:00"
                createdBy = 42L
                modifiedOn = "2024-06-01T00:00:00"
                modifiedBy = 99L
            }
            val dto = bookDAO.toBookDto()
            assertEquals("2024-01-01T00:00:00", dto.createdOn)
            assertEquals(42L, dto.createdBy)
            assertEquals("2024-06-01T00:00:00", dto.modifiedOn)
            assertEquals(99L, dto.modifiedBy)
        }
    }

    // ── userId field ──────────────────────────────────────────────────────────

    @Test
    fun testUserIdField() {
        val authorDAO = makeAuthor()
        transaction {
            val bookDAO = BookDAO.new {
                name = "User Book"
                author = authorDAO
                pages = 100
                isbn = "isbn-user"
                publishedDate = "2000-01-01"
                publisher = "Pub"
                quantity = 1
                image = null
                userId = 7L
            }
            assertEquals(7L, bookDAO.toBookDto().userId)
        }
    }

    // ── updateFromDto ─────────────────────────────────────────────────────────

    @Test
    fun testUpdateFromDto() {
        val author1 = makeAuthor(name = "Author 1", image = "img1")
        val author2 = makeAuthor(name = "Author 2", image = "img2")

        transaction {
            val bookDAO = BookDAO.new {
                name = "Original Book"
                author = author1
                pages = 100
                isbn = "original_isbn"
                publishedDate = "2000-01-01"
                publisher = "Original Pub"
                quantity = 1
                image = null
            }

            val updatedBookDto = Book(
                id = bookDAO.id.value,
                name = "Updated Book",
                author = author2.toAuthorDto(),
                translator = "New Translator",
                pages = 200,
                isbn = "updated_isbn",
                publishedDate = "2020-01-01",
                publisher = "Updated Pub",
                quantity = 5,
                image = "updated_book_img.jpg"
            )

            bookDAO.updateFromDto(updatedBookDto, author2)

            assertEquals("Updated Book", bookDAO.name)
            assertEquals("Author 2", bookDAO.author.name)
            assertEquals("New Translator", bookDAO.translator)
            assertEquals(200, bookDAO.pages)
            assertEquals("updated_isbn", bookDAO.isbn)
            assertEquals("2020-01-01", bookDAO.publishedDate)
            assertEquals("Updated Pub", bookDAO.publisher)
            assertEquals(5, bookDAO.quantity)
            assertEquals("updated_book_img.jpg", bookDAO.image)
        }
    }

    // ── author audit fields ───────────────────────────────────────────────────

    @Test
    fun testAuthorAuditFields() {
        transaction {
            val authorDAO = AuthorDAO.new {
                name = "Audited Author"
                image = "img.jpg"
                userId = 10L
                createdOn = "2024-01-01T00:00:00"
                createdBy = 1L
                modifiedOn = "2024-06-01T00:00:00"
                modifiedBy = 2L
            }
            val dto = authorDAO.toAuthorDto()
            assertEquals(10L, dto.userId)
            assertEquals("2024-01-01T00:00:00", dto.createdOn)
            assertEquals(1L, dto.createdBy)
            assertEquals("2024-06-01T00:00:00", dto.modifiedOn)
            assertEquals(2L, dto.modifiedBy)
        }
    }

    // ── count ─────────────────────────────────────────────────────────────────

    @Test
    fun testCountByUserId() {
        val authorDAO = makeAuthor()
        transaction {
            BookDAO.new {
                name = "Book 1"
                author = authorDAO
                isbn = "isbn-1"
                pages = 100
                publishedDate = "2000-01-01"
                publisher = "Pub"
                quantity = 1
                userId = 10L
            }
            BookDAO.new {
                name = "Book 2"
                author = authorDAO
                isbn = "isbn-2"
                pages = 100
                publishedDate = "2000-01-01"
                publisher = "Pub"
                quantity = 1
                userId = 10L
            }
            BookDAO.new {
                name = "Book 3"
                author = authorDAO
                isbn = "isbn-3"
                pages = 100
                publishedDate = "2000-01-01"
                publisher = "Pub"
                quantity = 1
                userId = 20L
            }

            assertEquals(2, BookDAO.find { BookTable.userId eq 10L }.count())
            assertEquals(1, BookDAO.find { BookTable.userId eq 20L }.count())
            assertEquals(0, BookDAO.find { BookTable.userId eq 30L }.count())
        }
    }
}
