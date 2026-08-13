package com.dw.service.member

import com.dw.db.MemberRepository
import com.dw.model.dto.Member

interface MemberServiceInterface {
    suspend fun getMembersByUserId(userId: Long): List<Member>
    suspend fun getMemberById(id: Long): Member?
    suspend fun createMember(member: Member): Member
    suspend fun updateMember(id: Long, member: Member): Member?
    suspend fun deleteMember(id: Long): Boolean
}

class MemberServiceImpl(private val memberRepository: MemberRepository) : MemberServiceInterface {

    override suspend fun getMembersByUserId(userId: Long): List<Member> =
        memberRepository.findByUserId(userId)

    override suspend fun getMemberById(id: Long): Member? = memberRepository.findById(id)

    override suspend fun createMember(member: Member): Member = memberRepository.save(member)

    override suspend fun updateMember(id: Long, member: Member): Member? =
        memberRepository.update(id, member)

    override suspend fun deleteMember(id: Long): Boolean = memberRepository.delete(id)
}
