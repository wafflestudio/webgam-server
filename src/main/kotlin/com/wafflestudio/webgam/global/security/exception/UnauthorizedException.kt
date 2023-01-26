package com.wafflestudio.webgam.global.security.exception

import com.wafflestudio.webgam.global.common.exception.ErrorType
import com.wafflestudio.webgam.global.common.exception.WebgamException

class UnauthorizedException: WebgamException.Unauthorized(ErrorType.Unauthorized.DEFAULT,
    "You have to first either login or signup.")