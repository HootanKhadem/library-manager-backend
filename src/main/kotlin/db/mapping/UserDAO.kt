package com.dw.db.mapping

import com.dw.model.dto.Role
import com.dw.model.dto.UserDTO
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

object UserTable : LongIdTable("user") {
    val username = varchar("username", 255)
    val password = varchar("password", 255)
    val email = varchar("email", 255)
    val role = varchar("role", 255)
    val salt = varchar("salt", 255)
}

class UserDAO(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<UserDAO>(UserTable)
    var username by UserTable.username
    var password by UserTable.password
    var email by UserTable.email
    var role by UserTable.role
    var salt by UserTable.salt

    fun toUserDto(): UserDTO = UserDTO(
        id = id.value,
        name = username,
        email = email,
        password = password,
        salt = salt,
        role = Role.valueOf(role)
    )

    fun updateFromDto(dto: UserDTO) {
        this.username = dto.name
        this.email = dto.email
        this.password = dto.password
        this.role = dto.role.name
        this.salt = dto.salt ?: ""
    }
}
