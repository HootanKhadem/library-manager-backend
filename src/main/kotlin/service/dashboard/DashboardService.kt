package com.dw.service.dashboard

import com.dw.db.BookRepository
import com.dw.db.GenreRepository
import com.dw.db.LendingRepository
import com.dw.db.UserActivityLogRepository
import com.dw.model.dto.dashboard.ActivityLogEntry
import com.dw.model.dto.dashboard.BookStats
import com.dw.model.dto.dashboard.LentOutStats
import com.dw.model.dto.dashboard.OverdueStats
import com.dw.model.dto.dashboard.RecentlyAddedBook
import java.time.YearMonth

interface DashboardServiceInterface {
    suspend fun getBookStats(userId: Long): BookStats
    suspend fun getLentOutStats(userId: Long): LentOutStats
    suspend fun getOverdueStats(userId: Long): OverdueStats
    suspend fun getRecentlyAdded(userId: Long, limit: Int = 5): List<RecentlyAddedBook>
    suspend fun getRecentActivity(userId: Long, limit: Int = 5): List<ActivityLogEntry>
}

class DashboardServiceImpl(
    private val bookRepository: BookRepository,
    private val lendingRepository: LendingRepository,
    private val genreRepository: GenreRepository,
    private val activityLogRepository: UserActivityLogRepository
) : DashboardServiceInterface {

    override suspend fun getBookStats(userId: Long): BookStats {
        val total = bookRepository.countByUserId(userId)
        val thisMonth = bookRepository.countAddedThisMonth(userId, YearMonth.now().toString())
        return BookStats(totalBooks = total, addedThisMonth = thisMonth)
    }

    override suspend fun getLentOutStats(userId: Long): LentOutStats {
        val totalLentOut = lendingRepository.countActiveByUserId(userId)
        val uniqueLendees = lendingRepository.countUniqueLendeesByUserId(userId)
        return LentOutStats(totalLentOut = totalLentOut, uniqueLendees = uniqueLendees)
    }

    override suspend fun getOverdueStats(userId: Long): OverdueStats =
        OverdueStats(totalOverdue = lendingRepository.countOverdueByUserId(userId))

    override suspend fun getRecentlyAdded(userId: Long, limit: Int): List<RecentlyAddedBook> {
        val genres = genreRepository.findByUserId(userId).associateBy { it.id }
        return bookRepository.findRecentlyAdded(userId, limit).map { book ->
            RecentlyAddedBook(
                id = book.id!!,
                name = book.name,
                author = book.author.name,
                genre = book.genreId?.let { genres[it]?.name },
                status = book.status,
                rating = book.rating
            )
        }
    }

    override suspend fun getRecentActivity(userId: Long, limit: Int): List<ActivityLogEntry> =
        activityLogRepository.findRecentByUserId(userId, limit).map { log ->
            ActivityLogEntry(
                id = log.id!!,
                action = log.action,
                bookName = log.bookName,
                memberName = log.memberName,
                occurredAt = log.createdOn
            )
        }
}
