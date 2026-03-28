package com.dw.plugins

import com.dw.service.authentication.JwtValidatorService
import com.dw.service.authentication.LoginServiceImpl
import com.dw.service.authentication.LoginServiceInterface
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*


fun Application.configureDependencyInjection() {
    dependencies {
        provide(JwtValidatorService::class)
        provide<LoginServiceInterface> { LoginServiceImpl() }

    }
}