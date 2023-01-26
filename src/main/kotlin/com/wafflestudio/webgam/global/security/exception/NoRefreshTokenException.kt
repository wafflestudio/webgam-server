package com.wafflestudio.webgam.global.security.exception

import com.wafflestudio.webgam.global.common.exception.ErrorType
import com.wafflestudio.webgam.global.common.exception.WebgamException

class NoRefreshTokenException: WebgamException.BadRequest(ErrorType.BadRequest.NO_REFRESH_TOKEN,
    "No refresh token is found in the cookie. Check if you are using secure connection(https).")