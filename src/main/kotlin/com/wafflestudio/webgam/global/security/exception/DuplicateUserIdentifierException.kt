package com.wafflestudio.webgam.global.security.exception

import com.wafflestudio.webgam.global.common.exception.ErrorType
import com.wafflestudio.webgam.global.common.exception.WebgamException

class DuplicateUserIdentifierException: WebgamException.Conflict(ErrorType.Conflict.DUPLICATE_USER_IDENTIFIER,
    "User with this id or email already exists.")