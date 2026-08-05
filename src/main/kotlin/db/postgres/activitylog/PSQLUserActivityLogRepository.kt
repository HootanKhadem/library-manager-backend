package com.dw.db.postgres.activitylog

import com.dw.db.UserActivityLogRepository
import com.dw.db.mapping.UserActivityLogDAO
import com.dw.db.mapping.UserActivityLogTable
import com.dw.db.withTransaction
import com.dw.model.dto.UserActivityLog
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import java.time.LocalDateTime

class PSQLUserActivityLogRepository : UserActivityLogRepository {

    override suspend fun save(log: UserActivityLog): UserActivityLog = withTransaction {
        val now = LocalDateTime.now().toString()
        UserActivityLogDAO.new {
            userId = log.userId
            userName = log.userName
            action = log.action
            bookId = log.bookId
            bookName = log.bookName
            memberId = log.memberId
            memberName = log.memberName
            createdOn = now
            createdBy = log.userId
            modifiedOn = now
            modifiedBy = log.userId
        }.toDto()
    }

    override suspend fun findRecentByUserId(userId: Long, limit: Int): List<UserActivityLog> = withTransaction {
        UserActivityLogDAO.find { UserActivityLogTable.userId eq userId }
            .orderBy(UserActivityLogTable.createdOn to SortOrder.DESC)
            .limit(limit)
            .map { it.toDto() }
    }
}
