package com.dw.service.openLib

import com.dw.model.dto.openlib.OpenLibSearchDoc

interface OpenLibApi {
    fun getBookInfo(isbn: String): OpenLibSearchDoc
}
