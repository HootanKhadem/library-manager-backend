package com.dw.db

import com.dw.model.dto.UserPreference

interface UserPreferenceRepository {
    suspend fun findByUserId(userId: Long): UserPreference?
    suspend fun upsert(userId: Long, preference: UserPreference): UserPreference
}
