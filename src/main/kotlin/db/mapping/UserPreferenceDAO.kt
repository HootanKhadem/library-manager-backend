package com.dw.db.mapping

import com.dw.model.dto.UserPreference
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

object UserPreferenceTable : LongIdTable("user_preference") {
    val userId = long("user_id").uniqueIndex()
    val libraryName = varchar("library_name", 255).nullable()
    val ownerName = varchar("owner_name", 255).nullable()
    val description = text("description").nullable()
    val defaultLoanDurationDays = integer("default_loan_duration_days").default(30)
    val dateFormat = varchar("date_format", 50).default("DD MMM YYYY")
    val language = varchar("language", 10).default("en")
    val createdOn = varchar("created_on", 255).nullable()
    val createdBy = long("created_by").nullable()
    val modifiedOn = varchar("modified_on", 255).nullable()
    val modifiedBy = long("modified_by").nullable()
}

class UserPreferenceDAO(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<UserPreferenceDAO>(UserPreferenceTable)

    var userId by UserPreferenceTable.userId
    var libraryName by UserPreferenceTable.libraryName
    var ownerName by UserPreferenceTable.ownerName
    var description by UserPreferenceTable.description
    var defaultLoanDurationDays by UserPreferenceTable.defaultLoanDurationDays
    var dateFormat by UserPreferenceTable.dateFormat
    var language by UserPreferenceTable.language
    var createdOn by UserPreferenceTable.createdOn
    var createdBy by UserPreferenceTable.createdBy
    var modifiedOn by UserPreferenceTable.modifiedOn
    var modifiedBy by UserPreferenceTable.modifiedBy

    fun toDto(): UserPreference = UserPreference(
        libraryName = libraryName,
        ownerName = ownerName,
        description = description,
        defaultLoanDurationDays = defaultLoanDurationDays,
        dateFormat = dateFormat,
        language = language,
        modifiedOn = modifiedOn
    )

    fun updateFromDto(dto: UserPreference, actingUserId: Long) {
        this.libraryName = dto.libraryName
        this.ownerName = dto.ownerName
        this.description = dto.description
        this.defaultLoanDurationDays = dto.defaultLoanDurationDays
        this.dateFormat = dto.dateFormat
        this.language = dto.language
        this.modifiedOn = dto.modifiedOn
        this.modifiedBy = actingUserId
    }
}
