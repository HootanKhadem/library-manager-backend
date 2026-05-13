package com.dw.plugins

import com.dw.db.AuthorRepository
import com.dw.db.BookRepository
import com.dw.db.UserRepository
import com.dw.db.postgres.book.PSQLBookRepository
import com.dw.db.postgres.user.PSQLAuthorRepository
import com.dw.db.postgres.user.PSQLUserRepository
import com.dw.service.admin.CreateUserService
import com.dw.service.admin.CreateUserServiceInterface
import com.dw.service.authentication.JwtService
import com.dw.service.authentication.LoginServiceImpl
import com.dw.service.authentication.LoginServiceInterface
import com.dw.service.author.AuthorServiceInterface
import com.dw.service.author.AuthorServiceInterfaceImpl
import com.dw.service.book.BookServiceImpl
import com.dw.service.book.BookServiceInterface
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*


fun Application.configureDependencyInjection() {
    val jwtConfig = getJwtConfig()

    dependencies {
        val jwtService = JwtService(jwtConfig)
        provide { jwtService }
        provide<UserRepository> { PSQLUserRepository(this@configureDependencyInjection.environment.config) }
        provide<AuthorRepository> { PSQLAuthorRepository(this@configureDependencyInjection.environment.config) }
        provide<BookRepository> { PSQLBookRepository() }
        provide<LoginServiceInterface> { LoginServiceImpl(userRepository = resolve(), jwtService = jwtService) }
        provide<CreateUserServiceInterface> { CreateUserService(userRepository = resolve()) }
        provide<AuthorServiceInterface> { AuthorServiceInterfaceImpl(authorRepository = resolve()) }
        provide<BookServiceInterface> { BookServiceImpl(bookRepository = resolve(), authorService = resolve()) }
    }
}
