package com.dw.routes.aggregation

import com.dw.db.mapping.AuthorDAO
import com.dw.db.mapping.BookDAO
import com.dw.model.dto.Role
import com.google.gson.JsonArray
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.Test
import routes.BaseRouteTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DashboardRoutesTest : BaseRouteTest() {

    @Test
    fun testCountUserBooksWithHeader() = testApplication {
        setupLibraryApp()
        client.get("/") // Force application start
        val userId = 10L
        setupBooksForUser(userId, 3)

        val token = createToken(userId = userId, role = Role.USER)

        val response = client.get("/count-user-books") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("3", response.bodyAsText())
    }

    @Test
    fun testCountUserBooksWithCookie() = testApplication {
        setupLibraryApp()
        client.get("/") // Force application start
        val userId = 20L
        setupBooksForUser(userId, 5)

        val token = createToken(userId = userId, role = Role.USER)

        val response = client.get("/count-user-books") {
            header(HttpHeaders.Cookie, "access_token=$token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("5", response.bodyAsText())
    }

    @Test
    fun testCountUserBooksUnauthenticated() = testApplication {
        setupLibraryApp()
        
        val response = client.get("/count-user-books")
        
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testDashboardStatsBooksReturnsCountAndThisMonth() = testApplication {
        setupLibraryApp()
        client.get("/") // Force application start
        val userId = 30L
        setupBooksForUser(userId, 2)
        val token = createToken(userId = userId, role = Role.USER)

        val response = client.get("/api/dashboard/stats/books") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        @Suppress("UNCHECKED_CAST")
        val stats = gson.fromJson(response.bodyAsText(), Map::class.java) as Map<String, Any>
        assertEquals(2.0, stats["totalBooks"])
    }

    @Test
    fun testDashboardStatsLentOutReturnsZeroWhenNoLendings() = testApplication {
        setupLibraryApp()
        val token = createToken(userId = 40L, role = Role.USER)

        val response = client.get("/api/dashboard/stats/lent-out") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        @Suppress("UNCHECKED_CAST")
        val stats = gson.fromJson(response.bodyAsText(), Map::class.java) as Map<String, Any>
        assertEquals(0.0, stats["totalLentOut"])
        assertEquals(0.0, stats["uniqueLendees"])
    }

    @Test
    fun testDashboardStatsOverdueReturnsZeroWhenNone() = testApplication {
        setupLibraryApp()
        val token = createToken(userId = 50L, role = Role.USER)

        val response = client.get("/api/dashboard/stats/overdue") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        @Suppress("UNCHECKED_CAST")
        val stats = gson.fromJson(response.bodyAsText(), Map::class.java) as Map<String, Any>
        assertEquals(0.0, stats["totalOverdue"])
    }

    @Test
    fun testDashboardRecentlyAddedReturnsBooks() = testApplication {
        setupLibraryApp()
        client.get("/")
        val userId = 60L
        setupBooksForUser(userId, 3)
        val token = createToken(userId = userId, role = Role.USER)

        val response = client.get("/api/dashboard/recently-added?limit=3") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val books = gson.fromJson(response.bodyAsText(), JsonArray::class.java)
        assertEquals(3, books.size())
    }

    @Test
    fun testDashboardRecentActivityReturnsEmptyListWhenNoActivity() = testApplication {
        setupLibraryApp()
        val token = createToken(userId = 70L, role = Role.USER)

        val response = client.get("/api/dashboard/recent-activity?limit=5") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val activity = gson.fromJson(response.bodyAsText(), JsonArray::class.java)
        assertNotNull(activity)
        assertTrue(activity.isEmpty)
    }

    @Test
    fun testDashboardEndpointsReturn401WithoutToken() = testApplication {
        setupLibraryApp()
        listOf(
            "/api/dashboard/stats/books",
            "/api/dashboard/stats/lent-out",
            "/api/dashboard/stats/overdue",
            "/api/dashboard/recently-added",
            "/api/dashboard/recent-activity"
        ).forEach { path ->
            val response = client.get(path)
            assertEquals(HttpStatusCode.Unauthorized, response.status, "Expected 401 for $path")
        }
    }

    private fun setupBooksForUser(userId: Long, count: Int) {
        transaction {
            val authorDAO = AuthorDAO.new {
                name = "Test Author"
                image = "author.jpg"
            }
            repeat(count) { i ->
                BookDAO.new {
                    name = "Book $i"
                    author = authorDAO
                    isbn = "isbn-$userId-$i"
                    pages = 100
                    publishedDate = "2000-01-01"
                    publisher = "Pub"
                    quantity = 1
                    this.userId = userId
                }
            }
        }
    }
}
