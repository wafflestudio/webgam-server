package com.wafflestudio.webgam.global.security.exception

import com.wafflestudio.webgam.global.common.exception.ErrorType
import com.wafflestudio.webgam.global.common.exception.WebgamException

class NoAccessException: WebgamException.Forbidden(ErrorType.Forbidden.NO_ACCESS,
    "You have no access to this resource.")