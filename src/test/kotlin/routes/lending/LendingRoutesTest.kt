package routes.lending

import com.dw.db.mapping.AuthorDAO
import com.dw.db.mapping.BookDAO
import com.dw.db.mapping.MemberDAO
import com.dw.model.dto.Lending
import com.dw.model.dto.Role
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import routes.BaseRouteTest
import kotlin.test.*

class LendingRoutesTest : BaseRouteTest() {

    override val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:lending_route_test;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver",
        "ktor.jwt.secret" to "secret",
        "ktor.jwt.issuer" to "issuer",
        "ktor.jwt.audience" to "audience",
        "ktor.jwt.realm" to "realm"
    )

    private fun setupBookAndMember(userId: Long): Pair<Long, Long> = transaction {
        val author = AuthorDAO.new { name = "Author"; image = "img.jpg" }
        val book = BookDAO.new {
            name = "Test Book"; this.author = author; isbn = "isbn-lending-$userId"
            pages = 100; publishedDate = "2020-01-01"; publisher = "Pub"; quantity = 1
            this.userId = userId; status = "OWNED"
        }
        val member = MemberDAO.new {
            name = "Borrower"; email = "b$userId@b.com"; password = "pass"; this.userId = userId
        }
        Pair(book.id.value, member.id.value)
    }

    @Test
    fun `POST api lending lends a book and returns 201`() = testApplication {
        setupLibraryApp()
        client.get("/") // force app start
        val userId = 10L
        val token = createToken(userId = userId, role = Role.USER)
        val (bookId, memberId) = setupBookAndMember(userId)

        val response = client.post("/api/lending") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(Lending(bookId = bookId, memberId = memberId, lentDate = "2026-06-16")))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val saved = gson.fromJson(response.bodyAsText(), Lending::class.java)
        assertNotNull(saved.id)
        assertEquals("ACTIVE", saved.status)

        cleanup()
    }

    @Test
    fun `GET api lending active returns active lendings`() = testApplication {
        setupLibraryApp()
        client.get("/") // force app start
        val userId = 11L
        val token = createToken(userId = userId, role = Role.USER)
        val (bookId, memberId) = setupBookAndMember(userId)

        client.post("/api/lending") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(Lending(bookId = bookId, memberId = memberId, lentDate = "2026-06-16")))
        }

        val response = client.get("/api/lending/active") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val lendings = gson.fromJson(response.bodyAsText(), Array<Lending>::class.java)
        assertTrue(lendings.isNotEmpty())

        cleanup()
    }

    @Test
    fun `PUT api lending id return marks lending as returned`() = testApplication {
        setupLibraryApp()
        client.get("/") // force app start
        val userId = 12L
        val token = createToken(userId = userId, role = Role.USER)
        val (bookId, memberId) = setupBookAndMember(userId)

        val lendResponse = client.post("/api/lending") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(Lending(bookId = bookId, memberId = memberId, lentDate = "2026-06-16")))
        }
        val lent = gson.fromJson(lendResponse.bodyAsText(), Lending::class.java)

        val response = client.put("/api/lending/${lent.id}/return") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val returned = gson.fromJson(response.bodyAsText(), Lending::class.java)
        assertEquals("RETURNED", returned.status)

        cleanup()
    }

    @Test
    fun `GET api lending active returns 401 without token`() = testApplication {
        setupLibraryApp()
        val response = client.get("/api/lending/active")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
