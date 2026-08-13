package com.dw.db

import com.dw.model.dto.ExportJob

interface ExportJobRepository {
    suspend fun create(userId: Long, expiresOn: String): ExportJob
    suspend fun findById(id: Long): ExportJob?
    suspend fun findByIdAndUserId(id: Long, userId: Long): ExportJob?
    suspend fun findAllByUserId(userId: Long): List<ExportJob>
    suspend fun markRunning(id: Long)
    suspend fun markCompleted(id: Long, filePath: String)
    suspend fun markFailed(id: Long, error: String)
    suspend fun getFilePath(id: Long): String?
    suspend fun getExpiresOn(id: Long): String?
}
