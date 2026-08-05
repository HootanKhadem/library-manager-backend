package com.dw.db.postgres.member

import com.dw.db.MemberRepository
import com.dw.db.mapping.MemberDAO
import com.dw.db.mapping.MemberTable
import com.dw.db.withTransaction
import com.dw.model.dto.Member
import org.jetbrains.exposed.v1.core.eq
import java.time.LocalDateTime

class PSQLMemberRepository : MemberRepository {

    override suspend fun save(member: Member): Member = withTransaction {
        val now = LocalDateTime.now().toString()
        MemberDAO.new {
            name = member.name
            email = member.email
            password = member.password
            userId = member.userId
            createdOn = now
            createdBy = member.userId
            modifiedOn = now
            modifiedBy = member.userId
        }.toMemberDto()
    }

    override suspend fun findById(id: Long): Member? = withTransaction {
        MemberDAO.findById(id)?.toMemberDto()
    }

    override suspend fun findByUserId(userId: Long): List<Member> = withTransaction {
        MemberDAO.find { MemberTable.userId eq userId }.map { it.toMemberDto() }
    }

    override suspend fun update(id: Long, member: Member): Member? = withTransaction {
        MemberDAO.findById(id)?.apply {
            name = member.name
            email = member.email
            modifiedOn = LocalDateTime.now().toString()
            modifiedBy = member.userId
        }?.toMemberDto()
    }

    override suspend fun delete(id: Long): Boolean = withTransaction {
        val dao = MemberDAO.findById(id) ?: return@withTransaction false
        dao.delete()
        true
    }
}
