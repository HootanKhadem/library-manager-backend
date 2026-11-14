package com.dw.routes.pagination

import io.ktor.server.application.ApplicationCall

/** `sort` is a field name (validated by the repository against its own whitelist); `dir` is "asc" (default) or "desc". */
data class SortParams(val sort: String?, val desc: Boolean)

fun ApplicationCall.sortParams(): SortParams = SortParams(
    sort = request.queryParameters["sort"]?.trim()?.takeIf { it.isNotEmpty() },
    desc = request.queryParameters["dir"].equals("desc", ignoreCase = true),
)
