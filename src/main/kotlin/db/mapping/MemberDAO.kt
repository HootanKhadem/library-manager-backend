package com.dw.db.mapping

import com.dw.model.dto.Member
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

object MemberTable : LongIdTable("member") {
    val name = varchar("name", 255)
    val email = varchar("email", 255)
    val password = varchar("password", 255)
    val userId = long("user_id").nullable()
    val createdOn = varchar("created_on", 255).nullable()
    val createdBy = long("created_by").nullable()
    val modifiedOn = varchar("modified_on", 255).nullable()
    val modifiedBy = long("modified_by").nullable()
}

class MemberDAO(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<MemberDAO>(MemberTable)

    var name by MemberTable.name
    var email by MemberTable.email
    var password by MemberTable.password
    var userId by MemberTable.userId
    var createdOn by MemberTable.createdOn
    var createdBy by MemberTable.createdBy
    var modifiedOn by MemberTable.modifiedOn
    var modifiedBy by MemberTable.modifiedBy

    fun toMemberDto(): Member = Member(
        id = id.value,
        name = name,
        email = email,
        password = password,
        userId = userId,
        createdOn = createdOn,
        createdBy = createdBy,
        modifiedOn = modifiedOn,
        modifiedBy = modifiedBy
    )
}
