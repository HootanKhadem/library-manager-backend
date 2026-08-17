package com.dw.plugins

import io.ktor.server.response.*
import io.ktor.util.date.*

const val ACCESS_TOKEN_COOKIE = "access_token"
const val REFRESH_TOKEN_COOKIE = "refresh_token"

fun ResponseCookies.appendAuthCookie(name: String, value: String, expires: GMTDate) {
    append(
        name,
        value,
        secure = true,
        httpOnly = true,
        path = "/",
        expires = expires,
        extensions = mapOf("SameSite" to "None")
    )
}

fun ResponseCookies.clearAuthCookie(name: String) {
    append(
        name,
        "",
        secure = true,
        httpOnly = true,
        path = "/",
        expires = GMTDate.START,
        extensions = mapOf("SameSite" to "None")
    )
}
