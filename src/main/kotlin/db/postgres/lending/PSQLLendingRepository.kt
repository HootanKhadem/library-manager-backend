package com.dw.db.postgres.lending

import com.dw.db.LendingRepository
import com.dw.db.mapping.LendingDAO
import com.dw.db.mapping.LendingTable
import com.dw.db.withTransaction
import com.dw.model.dto.Lending
import com.dw.model.dto.LendingStatus
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import java.time.LocalDateTime

class PSQLLendingRepository : LendingRepository {

    override suspend fun save(lending: Lending): Lending = withTransaction {
        val now = LocalDateTime.now().toString()
        LendingDAO.new {
            bookId = lending.bookId
            memberId = lending.memberId
            userId = lending.userId ?: 0L
            lentDate = lending.lentDate
            expectedReturnDate = lending.expectedReturnDate
            actualReturnDate = lending.actualReturnDate
            status = lending.status
            createdOn = now
            createdBy = lending.userId
            modifiedOn = now
            modifiedBy = lending.userId
        }.toLendingDto()
    }

    override suspend fun findById(id: Long): Lending? = withTransaction {
        LendingDAO.findById(id)?.toLendingDto()
    }

    override suspend fun findAllByUserId(userId: Long): List<Lending> = withTransaction {
        LendingDAO.find { LendingTable.userId eq userId }.map { it.toLendingDto() }
    }

    override suspend fun findActiveByUserId(userId: Long): List<Lending> = withTransaction {
        LendingDAO.find {
            (LendingTable.userId eq userId) and (LendingTable.status eq LendingStatus.ACTIVE.name)
        }.map { it.toLendingDto() }
    }

    override suspend fun countActiveByUserId(userId: Long): Long = withTransaction {
        LendingDAO.find {
            (LendingTable.userId eq userId) and (LendingTable.status eq LendingStatus.ACTIVE.name)
        }.count()
    }

    override suspend fun countUniqueLendeesByUserId(userId: Long): Long = withTransaction {
        LendingDAO.find {
            (LendingTable.userId eq userId) and (LendingTable.status eq LendingStatus.ACTIVE.name)
        }.map { it.memberId }.distinct().count().toLong()
    }

    override suspend fun countOverdueByUserId(userId: Long): Long = withTransaction {
        val now = LocalDateTime.now().toString()
        LendingDAO.find {
            (LendingTable.userId eq userId) and
            (LendingTable.status eq LendingStatus.ACTIVE.name) and
            (LendingTable.expectedReturnDate less now)
        }.count()
    }

    override suspend fun markReturned(id: Long, returnedDate: String): Lending? = withTransaction {
        LendingDAO.findById(id)?.apply {
            actualReturnDate = returnedDate
            status = LendingStatus.RETURNED.name
            modifiedOn = LocalDateTime.now().toString()
        }?.toLendingDto()
    }
}
