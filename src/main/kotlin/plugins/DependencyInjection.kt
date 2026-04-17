package com.dw.plugins

import com.dw.db.UserRepository
import com.dw.db.postgres.user.PSQLUserRepository
import com.dw.service.admin.CreateUserService
import com.dw.service.admin.CreateUserServiceInterface
import com.dw.service.authentication.JwtService
import com.dw.service.authentication.LoginServiceImpl
import com.dw.service.authentication.LoginServiceInterface
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*


fun Application.configureDependencyInjection() {
    val jwtConfig = getJwtConfig()

    dependencies {
        val jwtService = JwtService(jwtConfig)
        provide { jwtService }
        provide<UserRepository> { PSQLUserRepository(this@configureDependencyInjection.environment.config) }
        provide<LoginServiceInterface> { LoginServiceImpl(userRepository = resolve(), jwtService = jwtService) }
        provide<CreateUserServiceInterface> { CreateUserService(userRepository = resolve()) }
    }
}