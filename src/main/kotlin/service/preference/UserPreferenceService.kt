package com.dw.service.preference

import com.dw.db.UserPreferenceRepository
import com.dw.model.dto.UserPreference
import com.dw.model.dto.UserPreferenceConstraints

interface UserPreferenceServiceInterface {
    suspend fun getPreferences(userId: Long): UserPreference
    suspend fun savePreferences(userId: Long, preference: UserPreference): UserPreference
    fun validatePreferences(preference: UserPreference)
}

class UserPreferenceServiceImpl(
    private val userPreferenceRepository: UserPreferenceRepository
) : UserPreferenceServiceInterface {

    override suspend fun getPreferences(userId: Long): UserPreference =
        userPreferenceRepository.findByUserId(userId) ?: UserPreference()

    override fun validatePreferences(preference: UserPreference) {
        require(preference.defaultLoanDurationDays > 0) {
            "defaultLoanDurationDays must be greater than 0"
        }
        require(preference.language in UserPreferenceConstraints.ALLOWED_LANGUAGES) {
            "language must be one of ${UserPreferenceConstraints.ALLOWED_LANGUAGES}"
        }
        require(preference.dateFormat in UserPreferenceConstraints.ALLOWED_DATE_FORMATS) {
            "dateFormat must be one of ${UserPreferenceConstraints.ALLOWED_DATE_FORMATS}"
        }
    }

    override suspend fun savePreferences(userId: Long, preference: UserPreference): UserPreference {
        validatePreferences(preference)
        return userPreferenceRepository.upsert(userId, preference)
    }
}
