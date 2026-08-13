package com.dw.db.postgres.export

import com.dw.db.ExportJobRepository
import com.dw.db.mapping.ExportJobDAO
import com.dw.db.mapping.ExportJobTable
import com.dw.db.withTransaction
import com.dw.model.dto.ExportJob
import com.dw.model.dto.ExportJobStatus
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import java.time.LocalDateTime

class PSQLExportJobRepository : ExportJobRepository {

    override suspend fun create(userId: Long, expiresOn: String): ExportJob = withTransaction {
        val now = LocalDateTime.now().toString()
        ExportJobDAO.new {
            this.userId = userId
            this.status = ExportJobStatus.PENDING.name
            this.createdOn = now
            this.expiresOn = expiresOn
        }.toDto()
    }

    override suspend fun findById(id: Long): ExportJob? = withTransaction {
        ExportJobDAO.findById(id)?.toDto()
    }

    override suspend fun findByIdAndUserId(id: Long, userId: Long): ExportJob? = withTransaction {
        ExportJobDAO.find { (ExportJobTable.id eq id) and (ExportJobTable.userId eq userId) }
            .firstOrNull()
            ?.toDto()
    }

    override suspend fun findAllByUserId(userId: Long): List<ExportJob> = withTransaction {
        ExportJobDAO.find { ExportJobTable.userId eq userId }.map { it.toDto() }
    }

    override suspend fun markRunning(id: Long) {
        withTransaction {
            ExportJobDAO.findById(id)?.status = ExportJobStatus.RUNNING.name
        }
    }

    override suspend fun markCompleted(id: Long, filePath: String) {
        withTransaction {
            ExportJobDAO.findById(id)?.apply {
                status = ExportJobStatus.COMPLETED.name
                this.filePath = filePath
                completedOn = LocalDateTime.now().toString()
            }
        }
    }

    override suspend fun markFailed(id: Long, error: String) {
        withTransaction {
            ExportJobDAO.findById(id)?.apply {
                status = ExportJobStatus.FAILED.name
                this.error = error
                completedOn = LocalDateTime.now().toString()
            }
        }
    }

    override suspend fun getFilePath(id: Long): String? = withTransaction {
        ExportJobDAO.findById(id)?.filePath
    }

    override suspend fun getExpiresOn(id: Long): String? = withTransaction {
        ExportJobDAO.findById(id)?.expiresOn
    }
}
