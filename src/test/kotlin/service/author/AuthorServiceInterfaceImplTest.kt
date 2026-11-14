package service.author

import com.dw.db.postgres.user.PSQLAuthorRepository
import com.dw.db.postgres.book.PSQLBookRepository
import com.dw.model.dto.Author
import com.dw.model.dto.Book
import com.dw.model.dto.MergeAuthorsRequest
import com.dw.plugins.configureDatabases
import com.dw.service.author.AuthorServiceInterfaceImpl
import io.ktor.server.config.MapApplicationConfig
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*

class AuthorServiceInterfaceImplTest {

    private val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:author_service_test;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver"
    )

    private val authorRepository = PSQLAuthorRepository()
    private val authorService = AuthorServiceInterfaceImpl(authorRepository)

    @BeforeTest
    fun setup() {
        configureDatabases(testConfig)
    }

    @AfterTest
    fun tearDown() {
        transaction { exec("DROP ALL OBJECTS") }
    }

    // ── createAuthor ──────────────────────────────────────────────────────────

    @Test
    fun `createAuthor should save author`() = runBlocking {
        val author = Author(name = "John Doe", image = "john.jpg", userId = 1L)
        val created = authorService.createAuthor(author)

        assertNotNull(created.id)
        assertEquals(author.name, created.name)
        assertEquals(author.image, created.image)
    }

    @Test
    fun `createAuthor throws when userId is null`(): Unit = runBlocking {
        assertFailsWith<IllegalArgumentException> {
            authorService.createAuthor(Author(name = "No Owner", image = "none.jpg"))
        }
    }

    @Test
    fun `createAuthor should persist audit fields`() = runBlocking {
        val author = Author(
            name = "Audit Author",
            image = "audit.jpg",
            userId = 5L,
            createdOn = "2024-01-01T00:00:00",
            createdBy = 1L,
            modifiedOn = "2024-01-01T00:00:00",
            modifiedBy = 1L
        )
        val created = authorService.createAuthor(author)

        assertEquals(5L, created.userId)
        assertEquals("2024-01-01T00:00:00", created.createdOn)
        assertEquals(1L, created.createdBy)
    }

    // ── searchAuthors ─────────────────────────────────────────────────────────

    @Test
    fun `searchAuthors should return matching authors`() = runBlocking {
        authorService.createAuthor(Author(name = "Stephen King", image = "king.jpg", userId = 1L))
        authorService.createAuthor(Author(name = "Stephenie Meyer", image = "meyer.jpg", userId = 1L))
        authorService.createAuthor(Author(name = "J.K. Rowling", image = "rowling.jpg", userId = 1L))

        val results = authorService.searchAuthors(1L, "Stephen")

        assertEquals(2, results.size)
        assertTrue(results.any { it.name == "Stephen King" })
        assertTrue(results.any { it.name == "Stephenie Meyer" })
    }

    @Test
    fun `searchAuthors with empty query should return all authors`() = runBlocking {
        authorService.createAuthor(Author(name = "Author 1", image = "1.jpg", userId = 1L))
        authorService.createAuthor(Author(name = "Author 2", image = "2.jpg", userId = 1L))

        val results = authorService.searchAuthors(1L, "")

        assertEquals(2, results.size)
    }

    @Test
    fun `searchAuthors with no match should return empty list`() = runBlocking {
        authorService.createAuthor(Author(name = "Author 1", image = "1.jpg", userId = 1L))

        val results = authorService.searchAuthors(1L, "Nonexistent")

        assertEquals(0, results.size)
    }

    // ── findOrCreateAuthor ────────────────────────────────────────────────────

    @Test
    fun `findOrCreateAuthor creates author when not exists`(): Unit = runBlocking {
        val author = Author(name = "Brand New", image = "new.jpg")
        val result = authorService.findOrCreateAuthor(author, userId = 1L)

        assertNotNull(result.id)
        assertEquals("Brand New", result.name)
        assertEquals(1L, result.userId)
        assertNotNull(result.createdOn)
    }

    @Test
    fun `findOrCreateAuthor matches by name ignoring case and surrounding spaces`() = runBlocking {
        val saved = authorService.createAuthor(Author(name = "Case Author", image = "c.jpg", userId = 1L))
        val result = authorService.findOrCreateAuthor(Author(name = "  case AUTHOR ", image = ""), userId = 1L)

        assertEquals(saved.id, result.id)
    }

    @Test
    fun `findOrCreateAuthor never reuses another user's author with the same name`() = runBlocking {
        val theirs = authorService.createAuthor(Author(name = "Shared Name", image = "s.jpg", userId = 2L))
        val mine = authorService.findOrCreateAuthor(Author(name = "Shared Name", image = ""), userId = 1L)

        assertNotEquals(theirs.id, mine.id)
        assertEquals(1L, mine.userId)
    }

    @Test
    fun `findOrCreateAuthor ignores an author id that belongs to another user`() = runBlocking {
        val theirs = authorService.createAuthor(Author(name = "Not Yours", image = "n.jpg", userId = 2L))
        val result = authorService.findOrCreateAuthor(Author(id = theirs.id, name = "Not Yours", image = ""), userId = 1L)

        assertNotEquals(theirs.id, result.id)
        assertEquals(1L, result.userId)
    }

    @Test
    fun `an explicit author id keeps two same-named authors distinct`() = runBlocking {
        val first = authorService.createAuthor(Author(name = "Twin", image = "", userId = 1L))
        val second = authorService.createAuthor(Author(name = "Twin", image = "", userId = 1L))

        assertEquals(second.id, authorService.findOrCreateAuthor(Author(id = second.id, name = "Twin", image = ""), userId = 1L).id)
        assertEquals(first.id, authorService.findOrCreateAuthor(Author(id = first.id, name = "Twin", image = ""), userId = 1L).id)
    }

    @Test
    fun `searchAuthors only returns the callers authors and includes book counts`() = runBlocking {
        authorService.createAuthor(Author(name = "Mine Searchable", image = "", userId = 1L))
        authorService.createAuthor(Author(name = "Theirs Searchable", image = "", userId = 2L))

        val results = authorService.searchAuthors(1L, "searchable")

        assertEquals(listOf("Mine Searchable"), results.map { it.name })
        assertEquals(0L, results[0].bookCount)
    }

    @Test
    fun `findOrCreateAuthor returns existing author by name`() = runBlocking {
        val saved = authorService.createAuthor(Author(name = "Existing Author", image = "ex.jpg", userId = 1L))
        val result = authorService.findOrCreateAuthor(Author(name = "Existing Author", image = "other.jpg"), userId = 1L)

        assertEquals(saved.id, result.id)
        // confirm no duplicate
        assertEquals(1, authorService.searchAuthors(1L, "Existing Author").size)
    }

    @Test
    fun `findOrCreateAuthor returns existing author by id`() = runBlocking {
        val saved = authorService.createAuthor(Author(name = "Id Author", image = "id.jpg", userId = 1L))
        val result = authorService.findOrCreateAuthor(Author(id = saved.id, name = "Id Author", image = "id.jpg"), userId = 1L)

        assertEquals(saved.id, result.id)
    }

    // ── mergeAuthors ──────────────────────────────────────────────────────────

    private suspend fun bookFor(author: Author, userId: Long, isbn: String) =
        PSQLBookRepository().save(
            Book(name = "B$isbn", author = author, pages = 1, isbn = isbn, publisher = "P", quantity = 1, userId = userId)
        )

    @Test
    fun `mergeAuthors moves books to the target, deletes sources and renames`(): Unit = runBlocking {
        val target = authorService.createAuthor(Author(name = "J. Tolkien", image = "", userId = 1L))
        val s1 = authorService.createAuthor(Author(name = "JRR Tolkien", image = "", userId = 1L))
        val s2 = authorService.createAuthor(Author(name = "Tolkien, J.R.R.", image = "", userId = 1L))
        bookFor(target, 1L, "1"); bookFor(s1, 1L, "2"); bookFor(s2, 1L, "3")

        val merged = authorService.mergeAuthors(1L, MergeAuthorsRequest(target.id!!, listOf(s1.id!!, s2.id!!), "J.R.R. Tolkien"))

        assertEquals(target.id, merged?.id)
        assertEquals("J.R.R. Tolkien", merged?.name)
        assertEquals(3L, merged?.bookCount)
        assertNull(authorService.getAuthorById(1L, s1.id!!))
        assertNull(authorService.getAuthorById(1L, s2.id!!))
        assertEquals(1L, authorService.getAllAuthorsPaged(1L, 1, 20).totalItems)
    }

    @Test
    fun `previewMerge reports titles shared by different authors and changes nothing`(): Unit = runBlocking {
        val target = authorService.createAuthor(Author(name = "T", image = "", userId = 1L))
        val source = authorService.createAuthor(Author(name = "S", image = "", userId = 1L))
        val other = PSQLBookRepository()
        other.save(Book(name = "The Hobbit", author = target, pages = 1, isbn = "h1", publisher = "P", quantity = 1, userId = 1L))
        other.save(Book(name = " the hobbit ", author = source, pages = 1, isbn = "h2", publisher = "P", quantity = 1, userId = 1L))
        other.save(Book(name = "Solo", author = source, pages = 1, isbn = "s1", publisher = "P", quantity = 1, userId = 1L))
        other.save(Book(name = "Twice", author = target, pages = 1, isbn = "t1", publisher = "P", quantity = 1, userId = 1L))
        other.save(Book(name = "Twice", author = target, pages = 1, isbn = "t2", publisher = "P", quantity = 1, userId = 1L))

        val preview = authorService.previewMerge(1L, MergeAuthorsRequest(target.id!!, listOf(source.id!!)))

        assertEquals(5L, preview?.bookCount)
        assertEquals(listOf("The Hobbit"), preview?.duplicateTitles)
        assertNotNull(authorService.getAuthorById(1L, source.id!!))
        assertNull(authorService.previewMerge(2L, MergeAuthorsRequest(target.id!!, listOf(source.id!!))))
    }

    @Test
    fun `mergeAuthors keeps the target name when none is given`(): Unit = runBlocking {
        val target = authorService.createAuthor(Author(name = "Keep Me", image = "", userId = 1L))
        val source = authorService.createAuthor(Author(name = "Drop Me", image = "", userId = 1L))

        val merged = authorService.mergeAuthors(1L, MergeAuthorsRequest(target.id!!, listOf(source.id!!)))

        assertEquals("Keep Me", merged?.name)
    }

    @Test
    fun `mergeAuthors returns null and changes nothing when an author belongs to another user`(): Unit = runBlocking {
        val mine = authorService.createAuthor(Author(name = "Mine", image = "", userId = 1L))
        val theirs = authorService.createAuthor(Author(name = "Theirs", image = "", userId = 2L))
        bookFor(theirs, 2L, "9")

        assertNull(authorService.mergeAuthors(1L, MergeAuthorsRequest(mine.id!!, listOf(theirs.id!!))))
        assertNull(authorService.mergeAuthors(1L, MergeAuthorsRequest(theirs.id!!, listOf(mine.id!!))))
        assertEquals(1L, authorService.getAuthorById(2L, theirs.id!!)?.bookCount)
        assertNotNull(authorService.getAuthorById(1L, mine.id!!))
    }

    @Test
    fun `mergeAuthors rejects empty sources, target as source and blank name`(): Unit = runBlocking {
        val a = authorService.createAuthor(Author(name = "A", image = "", userId = 1L))
        val b = authorService.createAuthor(Author(name = "B", image = "", userId = 1L))

        assertFailsWith<IllegalArgumentException> { authorService.mergeAuthors(1L, MergeAuthorsRequest(a.id!!, emptyList())) }
        assertFailsWith<IllegalArgumentException> { authorService.mergeAuthors(1L, MergeAuthorsRequest(a.id!!, listOf(a.id!!))) }
        assertFailsWith<IllegalArgumentException> { authorService.mergeAuthors(1L, MergeAuthorsRequest(a.id!!, listOf(b.id!!), "  ")) }
    }

    // ── getAllAuthorsPaged ────────────────────────────────────────────────────

    @Test
    fun `getAllAuthorsPaged returns paged wrapper with correct metadata`() = runBlocking {
        authorService.createAuthor(Author(name = "Page Author 1", image = "1.jpg", userId = 55L))
        authorService.createAuthor(Author(name = "Page Author 2", image = "2.jpg", userId = 55L))
        authorService.createAuthor(Author(name = "Page Author 3", image = "3.jpg", userId = 55L))

        val result = authorService.getAllAuthorsPaged(55L, page = 1, pageSize = 2)

        assertEquals(2, result.items.size)
        assertEquals(1, result.page)
        assertEquals(2, result.pageSize)
        assertEquals(3, result.totalItems)
        assertEquals(2, result.totalPages)
    }

    @Test
    fun `getAllAuthorsPaged returns empty items with totalPages of 1 when user has no authors`() = runBlocking {
        val result = authorService.getAllAuthorsPaged(999L, page = 1, pageSize = 20)

        assertEquals(0, result.items.size)
        assertEquals(0, result.totalItems)
        assertEquals(1, result.totalPages)
    }
}
