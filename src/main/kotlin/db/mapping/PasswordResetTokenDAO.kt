package com.dw.db.mapping

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

object PasswordResetTokenTable : LongIdTable("password_reset_token") {
    val userId = long("user_id")
    val tokenHash = varchar("token_hash", 64).uniqueIndex()
    val expiresOn = varchar("expires_on", 255)
}

class PasswordResetTokenDAO(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PasswordResetTokenDAO>(PasswordResetTokenTable)

    var userId by PasswordResetTokenTable.userId
    var tokenHash by PasswordResetTokenTable.tokenHash
    var expiresOn by PasswordResetTokenTable.expiresOn
}
