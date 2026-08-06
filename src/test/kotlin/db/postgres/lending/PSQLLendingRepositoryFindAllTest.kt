package db.postgres.lending

import com.dw.db.mapping.AuthorDAO
import com.dw.db.mapping.BookDAO
import com.dw.db.mapping.MemberDAO
import com.dw.db.postgres.lending.PSQLLendingRepository
import com.dw.model.dto.Lending
import com.dw.model.dto.LendingStatus
import com.dw.plugins.configureDatabases
import io.ktor.server.config.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*

class PSQLLendingRepositoryFindAllTest {

    private val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:lending_findall_test;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver"
    )

    private val repository = PSQLLendingRepository()

    @BeforeTest
    fun setup() {
        configureDatabases(testConfig)
    }

    @AfterTest
    fun tearDown() {
        transaction { exec("DROP ALL OBJECTS") }
    }

    // lending.book_id / lending.member_id are FK-constrained (see V3 migration), so book and
    // member rows must exist before a lending can reference them.
    private fun seedBook(userId: Long, suffix: String): Long = transaction {
        val author = AuthorDAO.new { name = "Author"; image = "img.jpg" }
        BookDAO.new {
            name = "Book $suffix"; this.author = author; isbn = "isbn-findall-$suffix"
            pages = 100; publishedDate = "2020-01-01"; publisher = "Pub"; quantity = 1
            this.userId = userId; status = "OWNED"
        }.id.value
    }

    private fun seedMember(userId: Long): Long = transaction {
        MemberDAO.new {
            name = "Member"; email = "member-findall-$userId@test.com"; password = "pass"; this.userId = userId
        }.id.value
    }

    @Test
    fun `findAllByUserId returns both active and returned lendings`() = runBlocking {
        val bookId1 = seedBook(1L, "a")
        val bookId2 = seedBook(1L, "b")
        val memberId = seedMember(1L)

        val active = repository.save(
            Lending(bookId = bookId1, memberId = memberId, userId = 1L, lentDate = "2026-01-01", status = LendingStatus.ACTIVE.name)
        )
        repository.save(
            Lending(bookId = bookId2, memberId = memberId, userId = 1L, lentDate = "2026-01-01", status = LendingStatus.ACTIVE.name)
        )
        repository.markReturned(active.id!!, "2026-02-01")

        val all = repository.findAllByUserId(1L)
        assertEquals(2, all.size)
        assertTrue(all.any { it.status == LendingStatus.RETURNED.name })
        assertTrue(all.any { it.status == LendingStatus.ACTIVE.name })
    }

    @Test
    fun `findAllByUserId excludes other users' lendings`() = runBlocking {
        val bookId1 = seedBook(1L, "a")
        val bookId2 = seedBook(2L, "b")
        val memberId = seedMember(1L)

        repository.save(Lending(bookId = bookId1, memberId = memberId, userId = 1L, lentDate = "2026-01-01"))
        repository.save(Lending(bookId = bookId2, memberId = memberId, userId = 2L, lentDate = "2026-01-01"))

        assertEquals(1, repository.findAllByUserId(1L).size)
    }
}
