package com.dw.model.dto

import kotlinx.serialization.Serializable

enum class ExportJobStatus {
    PENDING, RUNNING, COMPLETED, FAILED
}

@Serializable
data class ExportJob(
    val id: Long,
    val status: ExportJobStatus,
    val createdOn: String,
    val completedOn: String? = null,
    val error: String? = null
)
