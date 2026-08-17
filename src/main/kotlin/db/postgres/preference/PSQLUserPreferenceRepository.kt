package com.dw.db.postgres.preference

import com.dw.db.UserPreferenceRepository
import com.dw.db.mapping.UserPreferenceDAO
import com.dw.db.mapping.UserPreferenceTable
import com.dw.db.withTransaction
import com.dw.model.dto.UserPreference
import org.jetbrains.exposed.v1.core.eq
import java.time.LocalDateTime

class PSQLUserPreferenceRepository : UserPreferenceRepository {

    override suspend fun findByUserId(userId: Long): UserPreference? = withTransaction {
        UserPreferenceDAO.find { UserPreferenceTable.userId eq userId }
            .firstOrNull()
            ?.toDto()
    }

    override suspend fun upsert(userId: Long, preference: UserPreference): UserPreference = withTransaction {
        val now = LocalDateTime.now().toString()
        val existing = UserPreferenceDAO.find { UserPreferenceTable.userId eq userId }.firstOrNull()

        val dao = existing ?: UserPreferenceDAO.new {
            this.userId = userId
            this.createdOn = now
            this.createdBy = userId
        }

        dao.updateFromDto(preference.copy(modifiedOn = now), actingUserId = userId)
        dao.toDto()
    }

    override suspend fun seedDefaults(userId: Long) {
        withTransaction {
            val now = LocalDateTime.now().toString()
            UserPreferenceDAO.new {
                this.userId = userId
                this.createdOn = now
                this.createdBy = userId
                this.modifiedOn = now
                this.modifiedBy = userId
            }
        }
    }
}
