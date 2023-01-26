package com.wafflestudio.webgam.global.security.exception

import com.wafflestudio.webgam.global.common.exception.ErrorType
import com.wafflestudio.webgam.global.common.exception.WebgamException

class InvalidJwtException(detail: String): WebgamException.Unauthorized(ErrorType.Unauthorized.INVALID_JWT, detail)