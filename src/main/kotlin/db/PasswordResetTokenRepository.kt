package com.dw.db

import java.time.LocalDateTime

data class PasswordResetToken(val userId: Long, val expiresOn: LocalDateTime)

interface PasswordResetTokenRepository {
    suspend fun save(userId: Long, tokenHash: String, expiresOn: LocalDateTime)
    suspend fun findByHash(tokenHash: String): PasswordResetToken?
    suspend fun deleteByUserId(userId: Long)
}
