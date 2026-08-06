package com.dw.model.dto

import kotlinx.serialization.Serializable

object UserPreferenceConstraints {
    val ALLOWED_LANGUAGES = setOf("en", "fa")
    val ALLOWED_DATE_FORMATS = setOf("DD MMM YYYY", "MM/DD/YYYY", "DD/MM/YYYY", "YYYY-MM-DD")
}

@Serializable
data class UserPreference(
    val libraryName: String? = null,
    val ownerName: String? = null,
    val description: String? = null,
    val defaultLoanDurationDays: Int = 30,
    val dateFormat: String = "DD MMM YYYY",
    val language: String = "en",
    val modifiedOn: String? = null
)
