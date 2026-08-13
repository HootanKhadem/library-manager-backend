package com.dw.routes.export

import com.dw.service.export.ExportDownloadResult
import com.dw.service.export.ExportJobServiceInterface
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.exportRoutes(exportJobService: ExportJobServiceInterface) {
    route("/api/exports") {
        post {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val job = exportJobService.startExport(userId)
            call.respond(HttpStatusCode.Accepted, job)
        }

        get {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                ?: return@get call.respond(HttpStatusCode.Unauthorized)
            call.respond(exportJobService.listJobs(userId))
        }

        get("/{id}") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val job = exportJobService.getJob(id, userId)
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(job)
        }

        get("/{id}/download") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            when (val result = exportJobService.resolveDownload(id, userId)) {
                is ExportDownloadResult.NotFound -> call.respond(HttpStatusCode.NotFound)
                is ExportDownloadResult.NotReady -> call.respond(HttpStatusCode.Conflict)
                is ExportDownloadResult.Expired -> call.respond(HttpStatusCode.Gone)
                is ExportDownloadResult.Ready -> {
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment
                            .withParameter(ContentDisposition.Parameters.FileName, result.file.name)
                            .toString()
                    )
                    call.respondFile(result.file)
                }
            }
        }
    }
}
