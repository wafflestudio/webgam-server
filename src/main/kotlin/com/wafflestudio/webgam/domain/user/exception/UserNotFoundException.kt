package com.wafflestudio.webgam.domain.user.exception

import com.wafflestudio.webgam.global.common.exception.ErrorType
import com.wafflestudio.webgam.global.common.exception.WebgamException

class UserNotFoundException(id: Long): WebgamException.NotFound(ErrorType.NotFound.USER_NOT_FOUND,
    "User with id $id does not exists.")