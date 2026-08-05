package com.dw.db

import com.dw.model.dto.UserActivityLog

interface UserActivityLogRepository {
    suspend fun save(log: UserActivityLog): UserActivityLog
    suspend fun findRecentByUserId(userId: Long, limit: Int): List<UserActivityLog>
}
