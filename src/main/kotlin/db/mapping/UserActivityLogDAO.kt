package com.dw.db.mapping

import com.dw.model.dto.UserActivityLog
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

object UserActivityLogTable : LongIdTable("user_activity_log") {
    val userId = long("user_id")
    val userName = varchar("user_name", 255).nullable()
    val action = varchar("action", 50)
    val bookId = long("book_id").nullable()
    val bookName = varchar("book_name", 255).nullable()
    val memberId = long("member_id").nullable()
    val memberName = varchar("member_name", 255).nullable()
    val createdOn = varchar("created_on", 255).nullable()
    val createdBy = long("created_by").nullable()
    val modifiedOn = varchar("modified_on", 255).nullable()
    val modifiedBy = long("modified_by").nullable()
}

class UserActivityLogDAO(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<UserActivityLogDAO>(UserActivityLogTable)

    var userId by UserActivityLogTable.userId
    var userName by UserActivityLogTable.userName
    var action by UserActivityLogTable.action
    var bookId by UserActivityLogTable.bookId
    var bookName by UserActivityLogTable.bookName
    var memberId by UserActivityLogTable.memberId
    var memberName by UserActivityLogTable.memberName
    var createdOn by UserActivityLogTable.createdOn
    var createdBy by UserActivityLogTable.createdBy
    var modifiedOn by UserActivityLogTable.modifiedOn
    var modifiedBy by UserActivityLogTable.modifiedBy

    fun toDto(): UserActivityLog = UserActivityLog(
        id = id.value,
        userId = userId,
        userName = userName,
        action = action,
        bookId = bookId,
        bookName = bookName,
        memberId = memberId,
        memberName = memberName,
        createdOn = createdOn,
        createdBy = createdBy,
        modifiedOn = modifiedOn,
        modifiedBy = modifiedBy
    )
}
