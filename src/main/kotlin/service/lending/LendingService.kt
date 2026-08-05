package com.dw.service.lending

import com.dw.db.BookRepository
import com.dw.db.LendingRepository
import com.dw.db.UserActivityLogRepository
import com.dw.model.dto.ActivityAction
import com.dw.model.dto.Lending
import com.dw.model.dto.UserActivityLog
import java.time.LocalDateTime

interface LendingServiceInterface {
    suspend fun lendBook(lending: Lending): Lending
    suspend fun returnBook(lendingId: Long, userId: Long): Lending?
    suspend fun getActiveByUserId(userId: Long): List<Lending>
}

class LendingServiceImpl(
    private val lendingRepository: LendingRepository,
    private val bookRepository: BookRepository,
    private val activityLogRepository: UserActivityLogRepository
) : LendingServiceInterface {

    override suspend fun lendBook(lending: Lending): Lending {
        val now = LocalDateTime.now().toString()
        val saved = lendingRepository.save(lending)
        val book = bookRepository.findById(lending.bookId)
        if (book != null) {
            bookRepository.update(book.id!!, book.copy(status = "LENT_OUT"))
            activityLogRepository.save(
                UserActivityLog(
                    userId = lending.userId ?: 0L,
                    action = ActivityAction.LENT.name,
                    bookId = book.id,
                    bookName = book.name,
                    memberId = lending.memberId,
                    createdOn = now,
                    createdBy = lending.userId,
                    modifiedOn = now,
                    modifiedBy = lending.userId
                )
            )
        }
        return saved
    }

    override suspend fun returnBook(lendingId: Long, userId: Long): Lending? {
        val now = LocalDateTime.now().toString()
        val returned = lendingRepository.markReturned(lendingId, now) ?: return null
        val book = bookRepository.findById(returned.bookId)
        if (book != null) {
            bookRepository.update(book.id!!, book.copy(status = "OWNED"))
            activityLogRepository.save(
                UserActivityLog(
                    userId = userId,
                    action = ActivityAction.RETURNED.name,
                    bookId = book.id,
                    bookName = book.name,
                    memberId = returned.memberId,
                    createdOn = now,
                    createdBy = userId,
                    modifiedOn = now,
                    modifiedBy = userId
                )
            )
        }
        return returned
    }

    override suspend fun getActiveByUserId(userId: Long): List<Lending> =
        lendingRepository.findActiveByUserId(userId)
}
