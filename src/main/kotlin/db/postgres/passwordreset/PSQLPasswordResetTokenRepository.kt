package com.dw.db.postgres.passwordreset

import com.dw.db.PasswordResetToken
import com.dw.db.PasswordResetTokenRepository
import com.dw.db.mapping.PasswordResetTokenDAO
import com.dw.db.mapping.PasswordResetTokenTable
import com.dw.db.withTransaction
import org.jetbrains.exposed.v1.core.eq
import java.time.LocalDateTime

class PSQLPasswordResetTokenRepository : PasswordResetTokenRepository {

    override suspend fun save(userId: Long, tokenHash: String, expiresOn: LocalDateTime) {
        withTransaction {
            PasswordResetTokenDAO.new {
                this.userId = userId
                this.tokenHash = tokenHash
                this.expiresOn = expiresOn.toString()
            }
        }
    }

    override suspend fun findByHash(tokenHash: String): PasswordResetToken? = withTransaction {
        PasswordResetTokenDAO.find { PasswordResetTokenTable.tokenHash eq tokenHash }
            .firstOrNull()
            ?.let { PasswordResetToken(it.userId, LocalDateTime.parse(it.expiresOn)) }
    }

    override suspend fun deleteByUserId(userId: Long) {
        withTransaction {
            PasswordResetTokenDAO.find { PasswordResetTokenTable.userId eq userId }.forEach { it.delete() }
        }
    }
}
