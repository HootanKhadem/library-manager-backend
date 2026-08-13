package com.dw.service.export

import com.dw.db.BookRepository
import com.dw.db.ExportJobRepository
import com.dw.db.GenreRepository
import com.dw.db.LendingRepository
import com.dw.db.MemberRepository
import com.dw.model.dto.Book
import com.dw.model.dto.ExportJob
import com.dw.model.dto.ExportJobStatus
import com.dw.model.dto.Genre
import com.dw.model.dto.Lending
import com.dw.model.dto.Member
import com.dw.service.util.CsvWriter
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

sealed class ExportDownloadResult {
    data object NotFound : ExportDownloadResult()
    data object NotReady : ExportDownloadResult()
    data object Expired : ExportDownloadResult()
    data class Ready(val file: File) : ExportDownloadResult()
}

interface ExportJobServiceInterface {
    suspend fun startExport(userId: Long): ExportJob
    suspend fun getJob(id: Long, userId: Long): ExportJob?
    suspend fun listJobs(userId: Long): List<ExportJob>
    suspend fun resolveDownload(id: Long, userId: Long): ExportDownloadResult
}

class ExportJobServiceImpl(
    private val exportJobRepository: ExportJobRepository,
    private val bookRepository: BookRepository,
    private val memberRepository: MemberRepository,
    private val lendingRepository: LendingRepository,
    private val genreRepository: GenreRepository,
    private val exportDirectory: String,
    private val retention: Duration
) : ExportJobServiceInterface {

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(ExportJobServiceImpl::class.java)
    }

    private val jobScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            logger.error("Uncaught exception in export job coroutine", throwable)
        }
    )

    override suspend fun startExport(userId: Long): ExportJob {
        val expiresOn = LocalDateTime.now().plus(retention).toString()
        val job = exportJobRepository.create(userId, expiresOn)
        jobScope.launch { generate(job.id, userId) }
        return job
    }

    override suspend fun getJob(id: Long, userId: Long): ExportJob? =
        exportJobRepository.findByIdAndUserId(id, userId)

    override suspend fun listJobs(userId: Long): List<ExportJob> =
        exportJobRepository.findAllByUserId(userId)

    override suspend fun resolveDownload(id: Long, userId: Long): ExportDownloadResult {
        val job = exportJobRepository.findByIdAndUserId(id, userId)
            ?: return ExportDownloadResult.NotFound

        if (job.status != ExportJobStatus.COMPLETED) {
            return ExportDownloadResult.NotReady
        }

        val expiresOn = exportJobRepository.getExpiresOn(id)
        if (expiresOn != null && LocalDateTime.parse(expiresOn).isBefore(LocalDateTime.now())) {
            return ExportDownloadResult.Expired
        }

        val path = exportJobRepository.getFilePath(id) ?: return ExportDownloadResult.NotFound
        val file = File(path)
        if (!file.exists()) return ExportDownloadResult.NotFound

        return ExportDownloadResult.Ready(file)
    }

    private suspend fun generate(jobId: Long, userId: Long) {
        try {
            exportJobRepository.markRunning(jobId)

            val dir = File(exportDirectory).apply { mkdirs() }
            val zipFile = File(dir, "export_${userId}_${System.currentTimeMillis()}.zip")

            ZipOutputStream(zipFile.outputStream()).use { zip ->
                writeCsvEntry(zip, "books.csv", booksCsv(bookRepository.findAllByUserId(userId)))
                writeCsvEntry(zip, "members.csv", membersCsv(memberRepository.findByUserId(userId)))
                writeCsvEntry(zip, "lendings.csv", lendingsCsv(lendingRepository.findAllByUserId(userId)))
                writeCsvEntry(zip, "genres.csv", genresCsv(genreRepository.findByUserId(userId)))
            }

            exportJobRepository.markCompleted(jobId, zipFile.absolutePath)
        } catch (e: Exception) {
            try {
                exportJobRepository.markFailed(jobId, (e.message ?: "export failed").take(500))
            } catch (markFailedException: Exception) {
                // Best-effort: if even marking the job as failed throws (e.g. a transient
                // DB error while already handling a failure), swallow it here rather than
                // letting it propagate out of jobScope and leave the job stuck in RUNNING
                // forever. Log via SLF4J so the double-fault is still visible in aggregated
                // logs, instead of only on stderr.
                logger.error(
                    "ExportJobService: failed to mark job $jobId as FAILED after export error " +
                        "(${e.message}): ${markFailedException.message}",
                    markFailedException
                )
            }
        }
    }

    private fun writeCsvEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun booksCsv(books: List<Book>): String = CsvWriter.write(
        headers = listOf("id", "name", "author", "isbn", "pages", "publisher", "publishedDate", "quantity", "status"),
        rows = books.map {
            listOf(
                it.id?.toString(), it.name, it.author.name, it.isbn,
                it.pages.toString(), it.publisher, it.publishedDate, it.quantity.toString(), it.status
            )
        }
    )

    private fun membersCsv(members: List<Member>): String = CsvWriter.write(
        headers = listOf("id", "name", "email"),
        rows = members.map { listOf(it.id.toString(), it.name, it.email) }
    )

    private fun lendingsCsv(lendings: List<Lending>): String = CsvWriter.write(
        headers = listOf("id", "bookId", "memberId", "lentDate", "expectedReturnDate", "actualReturnDate", "status"),
        rows = lendings.map {
            listOf(
                it.id?.toString(), it.bookId.toString(), it.memberId.toString(),
                it.lentDate, it.expectedReturnDate, it.actualReturnDate, it.status
            )
        }
    )

    private fun genresCsv(genres: List<Genre>): String = CsvWriter.write(
        headers = listOf("id", "name"),
        rows = genres.map { listOf(it.id?.toString(), it.name) }
    )
}
