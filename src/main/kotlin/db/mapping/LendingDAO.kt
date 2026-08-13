package com.dw.db.mapping

import com.dw.model.dto.Lending
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

object LendingTable : LongIdTable("lending") {
    val bookId = long("book_id")
    val memberId = long("member_id")
    val userId = long("user_id")
    val lentDate = varchar("lent_date", 255)
    val expectedReturnDate = varchar("expected_return_date", 255).nullable()
    val actualReturnDate = varchar("actual_return_date", 255).nullable()
    val status = varchar("status", 50).default("ACTIVE")
    val createdOn = varchar("created_on", 255).nullable()
    val createdBy = long("created_by").nullable()
    val modifiedOn = varchar("modified_on", 255).nullable()
    val modifiedBy = long("modified_by").nullable()
}

class LendingDAO(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<LendingDAO>(LendingTable)

    var bookId by LendingTable.bookId
    var memberId by LendingTable.memberId
    var userId by LendingTable.userId
    var lentDate by LendingTable.lentDate
    var expectedReturnDate by LendingTable.expectedReturnDate
    var actualReturnDate by LendingTable.actualReturnDate
    var status by LendingTable.status
    var createdOn by LendingTable.createdOn
    var createdBy by LendingTable.createdBy
    var modifiedOn by LendingTable.modifiedOn
    var modifiedBy by LendingTable.modifiedBy

    fun toLendingDto(): Lending = Lending(
        id = id.value,
        bookId = bookId,
        memberId = memberId,
        userId = userId,
        lentDate = lentDate,
        expectedReturnDate = expectedReturnDate,
        actualReturnDate = actualReturnDate,
        status = status,
        createdOn = createdOn,
        createdBy = createdBy,
        modifiedOn = modifiedOn,
        modifiedBy = modifiedBy
    )
}
