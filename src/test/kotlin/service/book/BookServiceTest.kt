package service.book

import com.dw.db.postgres.book.PSQLBookRepository
import com.dw.db.postgres.lending.PSQLLendingRepository
import com.dw.db.postgres.member.PSQLMemberRepository
import com.dw.db.postgres.user.PSQLAuthorRepository
import com.dw.model.dto.Author
import com.dw.model.dto.Book
import com.dw.model.dto.Lending
import com.dw.model.dto.Member
import com.dw.plugins.configureDatabases
import com.dw.service.author.AuthorServiceInterfaceImpl
import com.dw.service.book.BookHasLendingHistoryException
import com.dw.service.book.BookServiceImpl
import io.ktor.server.config.MapApplicationConfig
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*

class BookServiceTest {

    private val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:book_service_test;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver"
    )

    private val authorRepository = PSQLAuthorRepository()
    private val authorService = AuthorServiceInterfaceImpl(authorRepository)
    private val bookRepository = PSQLBookRepository()
    private val lendingRepository = PSQLLendingRepository()
    private val memberRepository = PSQLMemberRepository()
    private val bookService = BookServiceImpl(bookRepository, authorService, lendingRepository)

    @BeforeTest
    fun setup() {
        configureDatabases(testConfig)
    }

    @AfterTest
    fun tearDown() {
        transaction { exec("DROP ALL OBJECTS") }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun bookRequest(
        authorName: String = "Test Author",
        authorId: Long? = null,
        isbn: String = "isbn-001",
        userId: Long? = null
    ) = Book(
        name = "Test Book",
        author = Author(id = authorId, name = authorName, image = "author.jpg"),
        translator = "Some Translator",
        pages = 300,
        isbn = isbn,
        publishedDate = "2020-01-01",
        publisher = "Test Publisher",
        quantity = 5,
        image = "book.jpg",
        userId = userId
    )

    // ── createBook: author creation ───────────────────────────────────────────

    @Test
    fun `createBook creates new author when author not in db`() = runBlocking {
        val request = bookRequest(authorName = "Brand New Author", isbn = "isbn-new-author")
        val created = bookService.createBook(request)

        assertNotNull(created.id)
        assertNotNull(created.author.id)
        assertEquals("Brand New Author", created.author.name)
        assertEquals("Test Book", created.name)
    }

    @Test
    fun `createBook reuses existing author by name`() = runBlocking {
        val existingAuthor = authorService.createAuthor(Author(name = "Existing Author", image = "ex.jpg"))

        val request = bookRequest(authorName = "Existing Author", isbn = "isbn-reuse")
        val created = bookService.createBook(request)

        assertEquals(existingAuthor.id, created.author.id)

        // confirm no duplicate author was created
        val foundAuthors = authorService.searchAuthors("Existing Author")
        assertEquals(1, foundAuthors.size)
    }

    @Test
    fun `createBook reuses existing author by id`() = runBlocking {
        val existingAuthor = authorService.createAuthor(Author(name = "Id Author", image = "id.jpg"))

        val request = bookRequest(authorName = "Id Author", authorId = existingAuthor.id, isbn = "isbn-by-id")
        val created = bookService.createBook(request)

        assertEquals(existingAuthor.id, created.author.id)
    }

    // ── createBook: audit fields ──────────────────────────────────────────────

    @Test
    fun `createBook sets createdOn and modifiedOn automatically`() = runBlocking {
        val request = bookRequest(isbn = "isbn-audit")
        val created = bookService.createBook(request)

        assertNotNull(created.createdOn)
        assertNotNull(created.modifiedOn)
        assertEquals(created.createdOn, created.modifiedOn)
    }

    @Test
    fun `createBook sets createdBy and modifiedBy from userId`() = runBlocking {
        val request = bookRequest(isbn = "isbn-audit-user", userId = 42L)
        val created = bookService.createBook(request)

        assertEquals(42L, created.createdBy)
        assertEquals(42L, created.modifiedBy)
        assertEquals(42L, created.userId)
    }

    @Test
    fun `createBook propagates userId to new author`() = runBlocking {
        val request = bookRequest(authorName = "New Author With User", isbn = "isbn-author-user", userId = 7L)
        val created = bookService.createBook(request)

        assertEquals(7L, created.author.userId)
    }

    // ── createBook: translator field ──────────────────────────────────────────

    @Test
    fun `createBook persists translator field`() = runBlocking {
        val request = bookRequest(isbn = "isbn-translator").copy(translator = "Jane Doe")
        val created = bookService.createBook(request)

        assertEquals("Jane Doe", created.translator)
    }

    @Test
    fun `createBook allows null translator`() = runBlocking {
        val request = bookRequest(isbn = "isbn-no-trans").copy(translator = null)
        val created = bookService.createBook(request)

        assertNull(created.translator)
    }

    // ── getBookById ───────────────────────────────────────────────────────────

    @Test
    fun `getBookById returns book when exists`() = runBlocking {
        val created = bookService.createBook(bookRequest(isbn = "isbn-get"))

        val found = bookService.getBookById(created.id!!)
        assertNotNull(found)
        assertEquals(created.id, found.id)
        assertEquals("Test Book", found.name)
    }

    @Test
    fun `getBookById returns null when not exists`() = runBlocking {
        val found = bookService.getBookById(999L)
        assertNull(found)
    }

    // ── getAllBooks ───────────────────────────────────────────────────────────

    @Test
    fun `getAllBooks returns books for given userId`() = runBlocking {
        bookService.createBook(bookRequest(isbn = "isbn-u1-a", userId = 1L))
        bookService.createBook(bookRequest(isbn = "isbn-u1-b", userId = 1L))
        bookService.createBook(bookRequest(isbn = "isbn-u2-a", userId = 2L))

        val books = bookService.getAllBooks(1L)
        assertEquals(2, books.size)
        assertTrue(books.all { it.userId == 1L })
    }

    // ── getAllBooksPaged ──────────────────────────────────────────────────────

    @Test
    fun `getAllBooksPaged returns paged wrapper with correct metadata`() = runBlocking {
        bookService.createBook(bookRequest(isbn = "isbn-page-1", userId = 50L))
        bookService.createBook(bookRequest(isbn = "isbn-page-2", userId = 50L))
        bookService.createBook(bookRequest(isbn = "isbn-page-3", userId = 50L))

        val result = bookService.getAllBooksPaged(50L, page = 1, pageSize = 2)

        assertEquals(2, result.items.size)
        assertEquals(1, result.page)
        assertEquals(2, result.pageSize)
        assertEquals(3, result.totalItems)
        assertEquals(2, result.totalPages)
    }

    @Test
    fun `getAllBooksPaged returns empty items with totalPages of 1 when user has no books`() = runBlocking {
        val result = bookService.getAllBooksPaged(999L, page = 1, pageSize = 20)

        assertEquals(0, result.items.size)
        assertEquals(0, result.totalItems)
        assertEquals(1, result.totalPages)
    }

    // ── deleteBook ────────────────────────────────────────────────────────────

    @Test
    fun `deleteBook removes book when requester is the owner`() {
        runBlocking {
            val created = bookService.createBook(bookRequest(isbn = "isbn-delete", userId = 1L))

            val deleted = bookService.deleteBook(created.id!!, requesterId = 1L)
            assertTrue(deleted)
            assertNull(bookService.getBookById(created.id!!))
        }
    }

    @Test
    fun `deleteBook returns false when book not exists`() {
        runBlocking {
            val deleted = bookService.deleteBook(9999L, requesterId = 1L)
            assertFalse(deleted)
        }
    }

    @Test
    fun `deleteBook returns false for non-owner`() {
        runBlocking {
            val created = bookService.createBook(bookRequest(isbn = "isbn-delete-not-owner", userId = 1L))

            val deleted = bookService.deleteBook(created.id!!, requesterId = 2L)
            assertFalse(deleted)
            assertNotNull(bookService.getBookById(created.id!!))
        }
    }

    @Test
    fun `deleteBook throws BookHasLendingHistoryException when book has lending history`() {
        runBlocking {
            val created = bookService.createBook(bookRequest(isbn = "isbn-delete-lent", userId = 1L))
            val member = memberRepository.save(
                Member(name = "Borrower", email = "borrower@example.com", password = "pass", userId = 1L)
            )
            lendingRepository.save(
                Lending(
                    bookId = created.id!!,
                    memberId = member.id,
                    userId = 1L,
                    lentDate = "2026-01-01"
                )
            )

            assertFailsWith<BookHasLendingHistoryException> {
                bookService.deleteBook(created.id!!, requesterId = 1L)
            }
            // book must remain untouched
            assertNotNull(bookService.getBookById(created.id!!))
        }
    }

    // ── updateBook ────────────────────────────────────────────────────────────

    @Test
    fun `updateBook updates book when requester is the owner`() {
        runBlocking {
            val created = bookService.createBook(bookRequest(isbn = "isbn-update-owner", userId = 1L))

            val updated = bookService.updateBook(
                created.id!!,
                created.copy(name = "Updated Name", userId = 1L),
                requesterId = 1L
            )

            assertNotNull(updated)
            assertEquals("Updated Name", updated.name)
        }
    }

    @Test
    fun `updateBook returns null for non-owner`() {
        runBlocking {
            val created = bookService.createBook(bookRequest(isbn = "isbn-update-not-owner", userId = 1L))

            val updated = bookService.updateBook(
                created.id!!,
                created.copy(name = "Hacked Name", userId = 1L),
                requesterId = 2L
            )

            assertNull(updated)
            assertEquals("Test Book", bookService.getBookById(created.id!!)?.name)
        }
    }

    @Test
    fun `updateBook returns null when book does not exist`() {
        runBlocking {
            val request = bookRequest(isbn = "isbn-update-missing", userId = 1L)
            val updated = bookService.updateBook(9999L, request, requesterId = 1L)
            assertNull(updated)
        }
    }

    @Test
    fun `updateBook ignores userId supplied in the request body and keeps the verified owner`() {
        runBlocking {
            val created = bookService.createBook(bookRequest(isbn = "isbn-update-userid-spoof", userId = 1L))

            val updated = bookService.updateBook(
                created.id!!,
                created.copy(name = "Retitled", userId = 999L),
                requesterId = 1L
            )

            assertNotNull(updated)
            assertEquals(1L, updated.userId)
            assertEquals(1L, bookService.getBookById(created.id!!)?.userId)
        }
    }

    @Test
    fun `updateBook pins modifiedBy to the requester regardless of body`() {
        runBlocking {
            val created = bookService.createBook(bookRequest(isbn = "isbn-update-modifiedby", userId = 1L))

            val updated = bookService.updateBook(
                created.id!!,
                created.copy(name = "Retitled", modifiedBy = 999L),
                requesterId = 1L
            )

            assertNotNull(updated)
            assertEquals(1L, updated.modifiedBy)
        }
    }

    @Test
    fun `updateBook creates new author owned by the verified owner, not the body userId`() {
        runBlocking {
            val created = bookService.createBook(bookRequest(isbn = "isbn-update-author-owner", userId = 1L))

            val updated = bookService.updateBook(
                created.id!!,
                created.copy(
                    author = Author(name = "Newly Created During Update", image = "new.jpg"),
                    userId = 999L
                ),
                requesterId = 1L
            )

            assertNotNull(updated)
            assertEquals(1L, updated.author.userId)
        }
    }

    // ── countBooks ────────────────────────────────────────────────────────────

    @Test
    fun `countBooks returns correct count for userId`() = runBlocking {
        bookService.createBook(bookRequest(isbn = "isbn-c1", userId = 100L))
        bookService.createBook(bookRequest(isbn = "isbn-c2", userId = 100L))
        bookService.createBook(bookRequest(isbn = "isbn-c3", userId = 200L))

        assertEquals(2, bookService.countBooks(100L))
        assertEquals(1, bookService.countBooks(200L))
        assertEquals(0, bookService.countBooks(300L))
    }
}
