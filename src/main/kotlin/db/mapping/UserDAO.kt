package com.dw.db.mapping

import com.dw.model.dto.Role
import com.dw.model.dto.UserDTO
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

object UserTable : LongIdTable("user") {
    val username = varchar("username", 255).uniqueIndex()
    val password = varchar("password", 255)
    val email = varchar("email", 255).uniqueIndex()
    val role = varchar("role", 255)
    val salt = varchar("salt", 255)
    val createdOn = varchar("created_on", 255).nullable()
    val createdBy = long("created_by").nullable()
    val modifiedOn = varchar("modified_on", 255).nullable()
    val modifiedBy = long("modified_by").nullable()
}

class UserDAO(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<UserDAO>(UserTable)
    var username by UserTable.username
    var password by UserTable.password
    var email by UserTable.email
    var role by UserTable.role
    var salt by UserTable.salt
    var createdOn by UserTable.createdOn
    var createdBy by UserTable.createdBy
    var modifiedOn by UserTable.modifiedOn
    var modifiedBy by UserTable.modifiedBy

    fun toUserDto(): UserDTO = UserDTO(
        id = id.value,
        name = username,
        email = email,
        password = password,
        salt = salt,
        role = Role.valueOf(role),
        createdOn = createdOn,
        createdBy = createdBy,
        modifiedOn = modifiedOn,
        modifiedBy = modifiedBy
    )

    fun updateFromDto(dto: UserDTO) {
        this.username = dto.name
        this.email = dto.email
        this.password = dto.password
        this.role = dto.role.name
        this.salt = dto.salt ?: ""
        this.createdOn = dto.createdOn
        this.createdBy = dto.createdBy
        this.modifiedOn = dto.modifiedOn
        this.modifiedBy = dto.modifiedBy
    }
}
