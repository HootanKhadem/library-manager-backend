package service.export

import com.dw.db.BookRepository
import com.dw.db.ExportJobRepository
import com.dw.db.mapping.UserDAO
import com.dw.db.postgres.book.PSQLBookRepository
import com.dw.db.postgres.export.PSQLExportJobRepository
import com.dw.db.postgres.genre.PSQLGenreRepository
import com.dw.db.postgres.lending.PSQLLendingRepository
import com.dw.db.postgres.member.PSQLMemberRepository
import com.dw.db.postgres.user.PSQLAuthorRepository
import com.dw.model.dto.Author
import com.dw.model.dto.Book
import com.dw.model.dto.ExportJobStatus
import com.dw.model.dto.Member
import com.dw.model.dto.Role
import com.dw.plugins.configureDatabases
import com.dw.service.export.ExportDownloadResult
import com.dw.service.export.ExportJobServiceImpl
import io.ktor.server.config.MapApplicationConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File
import java.time.Duration
import java.util.zip.ZipFile
import kotlin.test.*

class ExportJobServiceTest {

    private val testConfig = MapApplicationConfig(
        "ktor.psql-database.url" to "jdbc:h2:mem:export_service_test;DB_CLOSE_DELAY=-1",
        "ktor.psql-database.username" to "sa",
        "ktor.psql-database.password" to "",
        "ktor.psql-database.driver" to "org.h2.Driver"
    )

    private val exportDir = File(System.getProperty("java.io.tmpdir"), "export-service-test-${System.nanoTime()}")
    private val exportJobRepository = PSQLExportJobRepository()
    private val bookRepository = PSQLBookRepository()
    private val authorRepository = PSQLAuthorRepository()
    private val memberRepository = PSQLMemberRepository()
    private val lendingRepository = PSQLLendingRepository()
    private val genreRepository = PSQLGenreRepository()

    private val service = ExportJobServiceImpl(
        exportJobRepository = exportJobRepository,
        bookRepository = bookRepository,
        memberRepository = memberRepository,
        lendingRepository = lendingRepository,
        genreRepository = genreRepository,
        exportDirectory = exportDir.absolutePath,
        retention = Duration.ofHours(24)
    )

    @BeforeTest
    fun setup() {
        configureDatabases(testConfig)
    }

    @AfterTest
    fun tearDown() {
        transaction { exec("DROP ALL OBJECTS") }
        exportDir.deleteRecursively()
    }

    // export_job.user_id is FK-constrained (see V4 migration), so a user row must exist
    // before an export job can reference it.
    private fun createUser(userId: Long) {
        transaction {
            UserDAO.new {
                username = "user$userId"
                password = "password"
                email = "user$userId@example.com"
                role = Role.USER.name
                salt = "salt"
            }
        }
    }

    private suspend fun waitForCompletion(jobId: Long, userId: Long, timeoutMs: Long = 5000): ExportJobStatus {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            val job = service.getJob(jobId, userId)
            if (job?.status == ExportJobStatus.COMPLETED || job?.status == ExportJobStatus.FAILED) {
                return job.status
            }
            delay(50)
        }
        throw AssertionError("job $jobId did not finish within ${timeoutMs}ms")
    }

    @Test
    fun `startExport eventually produces a completed job with a zip containing all four CSVs`() = runBlocking {
        createUser(1L)
        val authorDao = transaction {
            com.dw.db.mapping.AuthorDAO.new { name = "Author"; image = "a.jpg" }
        }
        bookRepository.save(
            Book(
                name = "Book 1", author = Author(id = authorDao.id.value, name = "Author", image = "a.jpg"),
                pages = 100, isbn = "isbn-export-1", publishedDate = "2020-01-01", publisher = "Pub",
                quantity = 1, userId = 1L
            )
        )
        memberRepository.save(Member(name = "Member 1", email = "m1@example.com", userId = 1L))

        val job = service.startExport(1L)
        assertEquals(ExportJobStatus.PENDING, job.status)

        val finalStatus = waitForCompletion(job.id, 1L)
        assertEquals(ExportJobStatus.COMPLETED, finalStatus)

        val result = service.resolveDownload(job.id, 1L)
        assertTrue(result is ExportDownloadResult.Ready)
        val file = (result as ExportDownloadResult.Ready).file
        assertTrue(file.exists())

        ZipFile(file).use { zip ->
            val entryNames = zip.entries().asSequence().map { it.name }.toSet()
            assertEquals(setOf("books.csv", "members.csv", "lendings.csv", "genres.csv"), entryNames)

            val booksCsv = zip.getInputStream(zip.getEntry("books.csv")).bufferedReader().readText()
            assertTrue(booksCsv.contains("Book 1"))

            val membersCsv = zip.getInputStream(zip.getEntry("members.csv")).bufferedReader().readText()
            assertTrue(membersCsv.contains("Member 1"))
        }
    }

    @Test
    fun `resolveDownload returns NotFound for a different user`() = runBlocking {
        createUser(1L)
        createUser(2L)
        val job = service.startExport(1L)
        waitForCompletion(job.id, 1L)

        assertEquals(ExportDownloadResult.NotFound, service.resolveDownload(job.id, 2L))
    }

    @Test
    fun `resolveDownload returns NotReady before the job completes`() = runBlocking {
        createUser(1L)
        val job = service.startExport(1L)
        assertEquals(ExportDownloadResult.NotReady, service.resolveDownload(job.id, 1L))
        waitForCompletion(job.id, 1L)
        Unit
    }

    @Test
    fun `resolveDownload returns Expired once past the retention window`() = runBlocking {
        createUser(1L)
        val expiredService = ExportJobServiceImpl(
            exportJobRepository = exportJobRepository,
            bookRepository = bookRepository,
            memberRepository = memberRepository,
            lendingRepository = lendingRepository,
            genreRepository = genreRepository,
            exportDirectory = exportDir.absolutePath,
            retention = Duration.ofSeconds(-1)
        )

        val job = expiredService.startExport(1L)
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < 5000) {
            if (expiredService.getJob(job.id, 1L)?.status == ExportJobStatus.COMPLETED) break
            delay(50)
        }

        assertEquals(ExportDownloadResult.Expired, expiredService.resolveDownload(job.id, 1L))
    }

    @Test
    fun `listJobs returns only that user's jobs`() = runBlocking {
        createUser(1L)
        createUser(2L)
        val job1 = service.startExport(1L)
        val job2 = service.startExport(1L)
        val job3 = service.startExport(2L)

        assertEquals(2, service.listJobs(1L).size)
        assertEquals(1, service.listJobs(2L).size)

        // Wait for all background generation coroutines to finish before the test method
        // returns. jobScope isn't tied to this test's structured concurrency scope, so if
        // we don't wait, these coroutines keep running into the next test's freshly-cleaned
        // database and throw uncaught "table not found" exceptions on background threads.
        waitForCompletion(job1.id, 1L)
        waitForCompletion(job2.id, 1L)
        waitForCompletion(job3.id, 2L)
        Unit
    }

    @Test
    fun `generate swallows a second failure from markFailed instead of crashing the coroutine`() = runBlocking {
        createUser(1L)

        // Force the export itself to fail...
        val throwingBookRepository = object : BookRepository by bookRepository {
            override suspend fun findAllByUserId(userId: Long): List<Book> {
                throw RuntimeException("simulated export failure")
            }
        }
        // ...and force the catch block's own markFailed(...) call to fail too, simulating
        // the "double fault" (a transient DB error while already handling a failure) that
        // could otherwise propagate out of jobScope uncaught and leave the job stuck in
        // RUNNING forever, since jobScope has no CoroutineExceptionHandler.
        val throwingOnMarkFailed = object : ExportJobRepository by exportJobRepository {
            override suspend fun markFailed(id: Long, error: String) {
                throw RuntimeException("simulated double failure: markFailed itself failed")
            }
        }

        val doubleFaultService = ExportJobServiceImpl(
            exportJobRepository = throwingOnMarkFailed,
            bookRepository = throwingBookRepository,
            memberRepository = memberRepository,
            lendingRepository = lendingRepository,
            genreRepository = genreRepository,
            exportDirectory = exportDir.absolutePath,
            retention = Duration.ofHours(24)
        )

        val job = doubleFaultService.startExport(1L)
        assertEquals(ExportJobStatus.PENDING, job.status)

        // Give the background coroutine time to run: markRunning -> export fails ->
        // markFailed also fails -> swallowed. Since markFailed itself throws, the job can
        // never reach FAILED here; what matters is that (a) this doesn't crash the process
        // with an uncaught exception on a background thread, and (b) the real job row is
        // left in RUNNING rather than being corrupted, so a future retry/cleanup mechanism
        // can still find and reconcile it.
        delay(1000)

        val realStatus = exportJobRepository.findByIdAndUserId(job.id, 1L)?.status
        assertEquals(ExportJobStatus.RUNNING, realStatus)
    }

    @Test
    fun `generate truncates an overlong failure message before persisting it`() = runBlocking {
        createUser(1L)

        // export_job.error is VARCHAR(500) (see V4 migration). JDBC/Exposed exception
        // messages routinely exceed that (they often embed the full SQL statement), so the
        // service must truncate before calling markFailed or the markFailed call itself
        // throws "value too long for type character varying(500)".
        val longMessage = "x".repeat(600)
        val throwingBookRepository = object : BookRepository by bookRepository {
            override suspend fun findAllByUserId(userId: Long): List<Book> {
                throw RuntimeException(longMessage)
            }
        }

        val overlongFailureService = ExportJobServiceImpl(
            exportJobRepository = exportJobRepository,
            bookRepository = throwingBookRepository,
            memberRepository = memberRepository,
            lendingRepository = lendingRepository,
            genreRepository = genreRepository,
            exportDirectory = exportDir.absolutePath,
            retention = Duration.ofHours(24)
        )

        val job = overlongFailureService.startExport(1L)
        val finalStatus = waitForCompletion(job.id, 1L)
        assertEquals(ExportJobStatus.FAILED, finalStatus)

        val persistedError = exportJobRepository.findByIdAndUserId(job.id, 1L)?.error
        assertNotNull(persistedError)
        assertTrue(persistedError.length <= 500)
        assertEquals(longMessage.take(500), persistedError)
    }
}
