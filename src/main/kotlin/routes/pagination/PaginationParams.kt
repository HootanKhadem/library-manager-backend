package com.dw.routes.pagination

import io.ktor.server.application.ApplicationCall

data class PageParams(val page: Int, val pageSize: Int)

fun ApplicationCall.pageParams(): PageParams {
    val page = (request.queryParameters["page"]?.toIntOrNull() ?: 1).coerceAtLeast(1)
    val pageSize = (request.queryParameters["pageSize"]?.toIntOrNull() ?: 20).coerceIn(1, 100)
    return PageParams(page, pageSize)
}
