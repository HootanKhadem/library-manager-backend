package com.dw.plugins

import com.dw.db.AuthorRepository
import com.dw.db.BookRepository
import com.dw.db.GenreRepository
import com.dw.db.LendingRepository
import com.dw.db.MemberRepository
import com.dw.db.UserActivityLogRepository
import com.dw.db.UserRepository
import com.dw.db.postgres.activitylog.PSQLUserActivityLogRepository
import com.dw.db.postgres.book.PSQLBookRepository
import com.dw.db.postgres.genre.PSQLGenreRepository
import com.dw.db.postgres.lending.PSQLLendingRepository
import com.dw.db.postgres.member.PSQLMemberRepository
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
import com.dw.service.dashboard.DashboardServiceImpl
import com.dw.service.dashboard.DashboardServiceInterface
import com.dw.service.genre.GenreServiceImpl
import com.dw.service.genre.GenreServiceInterface
import com.dw.service.lending.LendingServiceImpl
import com.dw.service.lending.LendingServiceInterface
import com.dw.service.member.MemberServiceImpl
import com.dw.service.member.MemberServiceInterface
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*

fun Application.configureDependencyInjection() {
    val jwtConfig = getJwtConfig()

    dependencies {
        val jwtService = JwtService(jwtConfig)
        provide { jwtService }
        provide<UserRepository> { PSQLUserRepository(this@configureDependencyInjection.environment.config) }
        provide<AuthorRepository> { PSQLAuthorRepository() }
        provide<BookRepository> { PSQLBookRepository() }
        provide<GenreRepository> { PSQLGenreRepository() }
        provide<MemberRepository> { PSQLMemberRepository() }
        provide<LendingRepository> { PSQLLendingRepository() }
        provide<UserActivityLogRepository> { PSQLUserActivityLogRepository() }
        provide<LoginServiceInterface> { LoginServiceImpl(userRepository = resolve(), jwtService = jwtService) }
        provide<CreateUserServiceInterface> { CreateUserService(userRepository = resolve(), genreRepository = resolve()) }
        provide<AuthorServiceInterface> { AuthorServiceInterfaceImpl(authorRepository = resolve()) }
        provide<BookServiceInterface> { BookServiceImpl(bookRepository = resolve(), authorService = resolve()) }
        provide<GenreServiceInterface> { GenreServiceImpl(genreRepository = resolve()) }
        provide<MemberServiceInterface> { MemberServiceImpl(memberRepository = resolve()) }
        provide<LendingServiceInterface> { LendingServiceImpl(lendingRepository = resolve(), bookRepository = resolve(), activityLogRepository = resolve()) }
        provide<DashboardServiceInterface> { DashboardServiceImpl(bookRepository = resolve(), lendingRepository = resolve(), genreRepository = resolve(), activityLogRepository = resolve()) }
    }
}
