package com.dw.db.mapping

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity

object UserTable : LongIdTable("user") {
    val username = varchar("username", 255)
    val password = varchar("", 255)
    val email = varchar("email", 255)
    val role = varchar("role", 255)
    val salt = varchar("salt", 255)
}

class UserDAO(id: EntityID<Long>) : LongEntity(id) {
}