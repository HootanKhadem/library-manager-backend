package com.dw.db.mapping

import com.dw.db.mock.user.MockUserRepository
import com.dw.model.dto.Role
import com.dw.model.dto.UserDTO
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UserDAOTest {

    private val userRepository = MockUserRepository()

    @Test
    fun testMockUserRepo() = runBlocking {
        val username = "testuser"
        val user = userRepository.findByUsername(username)
        
        assertNotNull(user)
        assertEquals(username, user.name)
        assertEquals(Role.USER, user.role)
    }

    @Test
    fun testMockUserRepoNoUser() = runBlocking {
        val user = userRepository.findByUsername("no-user")
        assertEquals(null, user)
    }

    @Test
    fun testMockUserRepoSave() = runBlocking {
        val dto = UserDTO(null, "newuser", "new@mail.com", "pass", Role.ADMIN)
        val saved = userRepository.save(dto)
        
        assertNotNull(saved.id)
        assertEquals(dto.name, saved.name)
        assertEquals(dto.role, saved.role)
    }

    @Test
    fun testMockUserRepoUpdate() = runBlocking {
        val dto = UserDTO(1L, "updated", "u@mail.com", "pass", Role.ADMIN)
        val updated = userRepository.update(dto)
        
        assertEquals(dto, updated)
    }
}
