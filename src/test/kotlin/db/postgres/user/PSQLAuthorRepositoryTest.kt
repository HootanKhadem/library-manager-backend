package db.postgres.user

import com.dw.db.postgres.user.PSQLAuthorRepository
import com.dw.model.dto.Author
import com.dw.plugins.configureDatabases
import io.ktor.server.config.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*

class PSQLAuthorRepositoryTest {

    private val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:repo_author_test;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver"
    )

    private val repository = PSQLAuthorRepository()

    @BeforeTest
    fun setup() {
        configureDatabases(testConfig)
    }

    @AfterTest
    fun tearDown() {
        transaction { exec("DROP ALL OBJECTS") }
    }

    @Test
    fun `findAllByUserIdPaged returns first page ordered by id`() = runBlocking {
        repository.save(Author(name = "Author A1", image = "a1.jpg", userId = 7L))
        repository.save(Author(name = "Author A2", image = "a2.jpg", userId = 7L))
        repository.save(Author(name = "Author A3", image = "a3.jpg", userId = 7L))

        val page1 = repository.findAllByUserIdPaged(7L, page = 1, pageSize = 2)
        assertEquals(2, page1.size)
        assertEquals("Author A1", page1[0].name)
        assertEquals("Author A2", page1[1].name)
    }

    @Test
    fun `findAllByUserIdPaged returns second page`() = runBlocking {
        repository.save(Author(name = "Author A1", image = "a1.jpg", userId = 7L))
        repository.save(Author(name = "Author A2", image = "a2.jpg", userId = 7L))
        repository.save(Author(name = "Author A3", image = "a3.jpg", userId = 7L))

        val page2 = repository.findAllByUserIdPaged(7L, page = 2, pageSize = 2)
        assertEquals(1, page2.size)
        assertEquals("Author A3", page2[0].name)
    }

    @Test
    fun `findAllByUserIdPaged filters by userId`() = runBlocking {
        repository.save(Author(name = "Author U1", image = "u1.jpg", userId = 1L))
        repository.save(Author(name = "Author U2", image = "u2.jpg", userId = 2L))

        val page1 = repository.findAllByUserIdPaged(1L, page = 1, pageSize = 10)
        assertEquals(1, page1.size)
        assertEquals("Author U1", page1[0].name)
    }

    @Test
    fun `countByUserId returns correct count`() = runBlocking {
        repository.save(Author(name = "Author C1", image = "c1.jpg", userId = 9L))
        repository.save(Author(name = "Author C2", image = "c2.jpg", userId = 9L))
        repository.save(Author(name = "Author C3", image = "c3.jpg", userId = 10L))

        assertEquals(2, repository.countByUserId(9L))
        assertEquals(1, repository.countByUserId(10L))
        assertEquals(0, repository.countByUserId(11L))
    }
}
