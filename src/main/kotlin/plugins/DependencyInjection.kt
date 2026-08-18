package com.dw.plugins

import com.dw.db.AuthorRepository
import com.dw.db.BookRepository
import com.dw.db.ExportJobRepository
import com.dw.db.GenreRepository
import com.dw.db.LendingRepository
import com.dw.db.MemberRepository
import com.dw.db.UserActivityLogRepository
import com.dw.db.UserPreferenceRepository
import com.dw.db.UserRepository
import com.dw.db.postgres.activitylog.PSQLUserActivityLogRepository
import com.dw.db.postgres.book.PSQLBookRepository
import com.dw.db.postgres.export.PSQLExportJobRepository
import com.dw.db.postgres.genre.PSQLGenreRepository
import com.dw.db.postgres.lending.PSQLLendingRepository
import com.dw.db.postgres.member.PSQLMemberRepository
import com.dw.db.postgres.preference.PSQLUserPreferenceRepository
import com.dw.db.postgres.user.PSQLAuthorRepository
import com.dw.db.postgres.user.PSQLUserRepository
import com.dw.service.admin.CreateUserService
import com.dw.service.admin.CreateUserServiceInterface
import com.dw.service.authentication.JwtService
import com.dw.service.authentication.LoginServiceImpl
import com.dw.service.authentication.LoginServiceInterface
import com.dw.service.authentication.SignupService
import com.dw.service.authentication.SignupServiceInterface
import com.dw.service.author.AuthorServiceInterface
import com.dw.service.author.AuthorServiceInterfaceImpl
import com.dw.service.book.BookServiceImpl
import com.dw.service.book.BookServiceInterface
import com.dw.service.dashboard.DashboardServiceImpl
import com.dw.service.dashboard.DashboardServiceInterface
import com.dw.service.export.ExportJobServiceImpl
import com.dw.service.export.ExportJobServiceInterface
import com.dw.service.genre.GenreServiceImpl
import com.dw.service.genre.GenreServiceInterface
import com.dw.service.lending.LendingServiceImpl
import com.dw.service.lending.LendingServiceInterface
import com.dw.service.member.MemberServiceImpl
import com.dw.service.member.MemberServiceInterface
import com.dw.service.preference.UserPreferenceServiceImpl
import com.dw.service.preference.UserPreferenceServiceInterface
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import java.time.Duration

fun Application.configureDependencyInjection() {
    val jwtConfig = getJwtConfig()
    val exportDirectory = this.environment.config.propertyOrNull("ktor.export.directory")?.getString() ?: "exports"
    val exportRetentionHours = this.environment.config.propertyOrNull("ktor.export.retentionHours")?.getString()?.toLongOrNull() ?: 24L

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
        provide<UserPreferenceRepository> { PSQLUserPreferenceRepository() }
        provide<LoginServiceInterface> { LoginServiceImpl(userRepository = resolve(), jwtService = jwtService) }
        provide<CreateUserServiceInterface> { CreateUserService(userRepository = resolve(), genreRepository = resolve(), userPreferenceRepository = resolve()) }
        provide<AuthorServiceInterface> { AuthorServiceInterfaceImpl(authorRepository = resolve()) }
        provide<BookServiceInterface> { BookServiceImpl(bookRepository = resolve(), authorService = resolve(), lendingRepository = resolve()) }
        provide<GenreServiceInterface> { GenreServiceImpl(genreRepository = resolve()) }
        provide<MemberServiceInterface> { MemberServiceImpl(memberRepository = resolve()) }
        provide<LendingServiceInterface> { LendingServiceImpl(lendingRepository = resolve(), bookRepository = resolve(), activityLogRepository = resolve()) }
        provide<DashboardServiceInterface> { DashboardServiceImpl(bookRepository = resolve(), lendingRepository = resolve(), genreRepository = resolve(), activityLogRepository = resolve()) }
        provide<UserPreferenceServiceInterface> { UserPreferenceServiceImpl(userPreferenceRepository = resolve()) }
        provide<SignupServiceInterface> {
            SignupService(
                userRepository = resolve(),
                genreRepository = resolve(),
                userPreferenceRepository = resolve(),
                userPreferenceService = resolve(),
                jwtService = jwtService
            )
        }
        provide<ExportJobRepository> { PSQLExportJobRepository() }
        provide<ExportJobServiceInterface> {
            ExportJobServiceImpl(
                exportJobRepository = resolve(),
                bookRepository = resolve(),
                memberRepository = resolve(),
                lendingRepository = resolve(),
                genreRepository = resolve(),
                exportDirectory = exportDirectory,
                retention = Duration.ofHours(exportRetentionHours)
            )
        }
    }
}
