package com.dw.plugins

import com.dw.model.dto.Role
import com.dw.routes.admin.adminCreateUser
import com.dw.routes.aggregation.countUserBooks
import com.dw.routes.aggregation.dashboardRoutes
import com.dw.routes.author.authorRoutes
import com.dw.routes.book.bookRoutes
import com.dw.routes.export.exportRoutes
import com.dw.routes.genre.genreRoutes
import com.dw.routes.helloWorld.helloWorld
import com.dw.routes.lending.lendingRoutes
import com.dw.routes.member.memberRoutes
import com.dw.routes.metrics.metrics
import com.dw.routes.preference.preferenceRoutes
import com.dw.routes.user.login
import com.dw.routes.user.logout
import com.dw.routes.user.signup
import com.dw.service.admin.CreateUserServiceInterface
import com.dw.service.authentication.LoginServiceInterface
import com.dw.service.authentication.SignupServiceInterface
import com.dw.service.author.AuthorServiceInterface
import com.dw.service.book.BookServiceInterface
import com.dw.service.dashboard.DashboardServiceInterface
import com.dw.service.export.ExportJobServiceInterface
import com.dw.service.genre.GenreServiceInterface
import com.dw.service.lending.LendingServiceInterface
import com.dw.service.member.MemberServiceInterface
import com.dw.service.preference.UserPreferenceServiceInterface
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.di.*
import io.ktor.server.routing.*

suspend fun Application.configurePublicRouting() {
    val loginService = dependencies.resolve<LoginServiceInterface>()
    val signupService = dependencies.resolve<SignupServiceInterface>()
    routing {
        helloWorld()
        metrics()
        login(loginService)
        logout()
        signup(signupService)
    }
}

suspend fun Application.configureAuthenticatedRouting() {
    val authorService = dependencies.resolve<AuthorServiceInterface>()
    val bookService = dependencies.resolve<BookServiceInterface>()
    val genreService = dependencies.resolve<GenreServiceInterface>()
    val memberService = dependencies.resolve<MemberServiceInterface>()
    val lendingService = dependencies.resolve<LendingServiceInterface>()
    val dashboardService = dependencies.resolve<DashboardServiceInterface>()
    val preferenceService = dependencies.resolve<UserPreferenceServiceInterface>()
    val exportJobService = dependencies.resolve<ExportJobServiceInterface>()

    routing {
        authenticate("auth-jwt") {
            withRole(Role.USER) {
                authorRoutes(authorService)
                bookRoutes(bookService)
                genreRoutes(genreService)
                memberRoutes(memberService)
                lendingRoutes(lendingService)
                countUserBooks(bookService)
                dashboardRoutes(dashboardService)
                preferenceRoutes(preferenceService)
                exportRoutes(exportJobService)
            }
        }
    }
}

suspend fun Application.configureAdminRouting() {
    val adminCreateUserService = dependencies.resolve<CreateUserServiceInterface>()
    routing {
        authenticate("auth-jwt") {
            withRole(Role.ADMIN) {
                adminCreateUser(adminCreateUserService)
            }
        }
    }
}
