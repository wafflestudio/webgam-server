package com.wafflestudio.webgam.global.common.dto

import com.wafflestudio.webgam.global.common.exception.WebgamException

data class ErrorResponse(
        val errorCode: Int,
        val errorMessage: String,
        val detail: String
) {
    constructor(webgamException: WebgamException): this(
        errorCode = webgamException.errorType.code(),
        errorMessage = webgamException.errorType.toString(),
        detail = webgamException.detail
    )
}