package com.wafflestudio.webgam.global.security.exception

import com.wafflestudio.webgam.global.common.exception.ErrorType
import com.wafflestudio.webgam.global.common.exception.WebgamException

class LoginFailedException: WebgamException.Unauthorized(ErrorType.Unauthorized.LOGIN_FAIL,
    "아이디나 패스워드가 일치하지 않습니다.")