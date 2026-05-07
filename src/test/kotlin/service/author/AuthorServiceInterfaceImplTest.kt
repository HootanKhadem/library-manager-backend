package service.author

import com.dw.db.mapping.AuthorTable
import com.dw.db.mapping.BookTable
import com.dw.db.mapping.UserTable
import com.dw.db.postgres.user.PSQLAuthorRepository
import com.dw.model.dto.Author
import com.dw.plugins.configureDatabases
import com.dw.service.author.AuthorServiceInterfaceImpl
import io.ktor.server.config.MapApplicationConfig
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
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
        transaction {
            SchemaUtils.drop(BookTable, AuthorTable, UserTable)
        }
    }

    // ── createAuthor ──────────────────────────────────────────────────────────

    @Test
    fun `createAuthor should save author`() = runBlocking {
        val author = Author(name = "John Doe", image = "john.jpg")
        val created = authorService.createAuthor(author)

        assertNotNull(created.id)
        assertEquals(author.name, created.name)
        assertEquals(author.image, created.image)
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
        authorService.createAuthor(Author(name = "Stephen King", image = "king.jpg"))
        authorService.createAuthor(Author(name = "Stephenie Meyer", image = "meyer.jpg"))
        authorService.createAuthor(Author(name = "J.K. Rowling", image = "rowling.jpg"))

        val results = authorService.searchAuthors("Stephen")

        assertEquals(2, results.size)
        assertTrue(results.any { it.name == "Stephen King" })
        assertTrue(results.any { it.name == "Stephenie Meyer" })
    }

    @Test
    fun `searchAuthors with empty query should return all authors`() = runBlocking {
        authorService.createAuthor(Author(name = "Author 1", image = "1.jpg"))
        authorService.createAuthor(Author(name = "Author 2", image = "2.jpg"))

        val results = authorService.searchAuthors("")

        assertEquals(2, results.size)
    }

    @Test
    fun `searchAuthors with no match should return empty list`() = runBlocking {
        authorService.createAuthor(Author(name = "Author 1", image = "1.jpg"))

        val results = authorService.searchAuthors("Nonexistent")

        assertEquals(0, results.size)
    }

    // ── findOrCreateAuthor ────────────────────────────────────────────────────

    @Test
    fun `findOrCreateAuthor creates author when not exists`() = runBlocking {
        val author = Author(name = "Brand New", image = "new.jpg")
        val result = authorService.findOrCreateAuthor(author, userId = 1L)

        assertNotNull(result.id)
        assertEquals("Brand New", result.name)
        assertEquals(1L, result.userId)
        assertNotNull(result.createdOn)
    }

    @Test
    fun `findOrCreateAuthor returns existing author by name`() = runBlocking {
        val saved = authorService.createAuthor(Author(name = "Existing Author", image = "ex.jpg"))
        val result = authorService.findOrCreateAuthor(Author(name = "Existing Author", image = "other.jpg"))

        assertEquals(saved.id, result.id)
        // confirm no duplicate
        assertEquals(1, authorService.searchAuthors("Existing Author").size)
    }

    @Test
    fun `findOrCreateAuthor returns existing author by id`() = runBlocking {
        val saved = authorService.createAuthor(Author(name = "Id Author", image = "id.jpg"))
        val result = authorService.findOrCreateAuthor(Author(id = saved.id, name = "Id Author", image = "id.jpg"))

        assertEquals(saved.id, result.id)
    }
}
