package com.dw.routes.admin

import com.dw.model.dto.UserDTO
import com.dw.service.admin.CreateUserServiceInterface
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.adminCreateUser(createUserService: CreateUserServiceInterface) {
    post("/admin/users") {
        val userDTO = call.receive<UserDTO>()
        val savedUser = createUserService.createNewUser(userDTO)
        call.respond(HttpStatusCode.Created, savedUser)
    }
}
