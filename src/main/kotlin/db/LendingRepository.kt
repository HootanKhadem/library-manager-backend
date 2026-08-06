package com.dw.db

import com.dw.model.dto.Lending

interface LendingRepository {
    suspend fun save(lending: Lending): Lending
    suspend fun findById(id: Long): Lending?
    suspend fun findAllByUserId(userId: Long): List<Lending>
    suspend fun findActiveByUserId(userId: Long): List<Lending>
    suspend fun countActiveByUserId(userId: Long): Long
    suspend fun countUniqueLendeesByUserId(userId: Long): Long
    suspend fun countOverdueByUserId(userId: Long): Long
    suspend fun markReturned(id: Long, returnedDate: String): Lending?
}
