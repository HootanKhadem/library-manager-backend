package com.dw.plugins

import com.dw.db.postgres.user.PSQLUserRepository
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
        provide<LoginServiceInterface> { LoginServiceImpl(jwtService = jwtService) }
        provide { PSQLUserRepository(this@configureDependencyInjection.environment.config) }
    }
}