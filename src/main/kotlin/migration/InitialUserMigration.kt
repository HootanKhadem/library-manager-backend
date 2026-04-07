package com.dw.migration

import com.dw.db.UserRepository
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*


suspend fun Application.initialUserMigration() {
    val userRepository = dependencies.resolve<UserRepository>()

    val user = userRepository.createAdminUser()
}