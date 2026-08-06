package com.dw.db.mapping

import com.dw.model.dto.ExportJob
import com.dw.model.dto.ExportJobStatus
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

object ExportJobTable : LongIdTable("export_job") {
    val userId = long("user_id")
    val status = varchar("status", 20).default(ExportJobStatus.PENDING.name)
    val filePath = varchar("file_path", 500).nullable()
    val error = varchar("error", 500).nullable()
    val createdOn = varchar("created_on", 255)
    val completedOn = varchar("completed_on", 255).nullable()
    val expiresOn = varchar("expires_on", 255).nullable()
}

class ExportJobDAO(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ExportJobDAO>(ExportJobTable)

    var userId by ExportJobTable.userId
    var status by ExportJobTable.status
    var filePath by ExportJobTable.filePath
    var error by ExportJobTable.error
    var createdOn by ExportJobTable.createdOn
    var completedOn by ExportJobTable.completedOn
    var expiresOn by ExportJobTable.expiresOn

    fun toDto(): ExportJob = ExportJob(
        id = id.value,
        status = ExportJobStatus.valueOf(status),
        createdOn = createdOn,
        completedOn = completedOn,
        error = error
    )
}
