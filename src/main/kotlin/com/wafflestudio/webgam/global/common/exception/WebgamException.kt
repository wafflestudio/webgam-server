package com.wafflestudio.webgam.global.common.exception

import com.wafflestudio.webgam.global.common.exception.ErrorType.ErrorTypeInterface

sealed class WebgamException(
    val errorType: ErrorTypeInterface,
    val detail: String
): RuntimeException() {
    abstract class BadRequest(errorType: ErrorType.BadRequest, detail: String): WebgamException(errorType, detail)
    abstract class Unauthorized(errorType: ErrorType.Unauthorized, detail: String): WebgamException(errorType, detail)
    abstract class Forbidden(errorType: ErrorType.Forbidden, detail: String): WebgamException(errorType, detail)
    abstract class NotFound(errorType: ErrorType.NotFound, detail: String): WebgamException(errorType, detail)
    abstract class Conflict(errorType: ErrorType.Conflict, detail: String): WebgamException(errorType, detail)
    abstract class ServerError(errorType: ErrorType.ServerError, detail: String): WebgamException(errorType, detail)
}