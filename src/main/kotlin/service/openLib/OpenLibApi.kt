package com.dw.service.openLib

import com.dw.model.dto.openlib.OpenLibDto

interface OpenLibApi {
    fun getBookInfo(isbn: String): OpenLibDto
}