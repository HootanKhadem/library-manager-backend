package com.dw.db

import com.dw.model.dto.Member

interface MemberRepository {
    suspend fun save(member: Member): Member
    suspend fun findById(id: Long): Member?
    suspend fun findByUserId(userId: Long): List<Member>
    suspend fun update(id: Long, member: Member): Member?
    suspend fun delete(id: Long): Boolean
}
