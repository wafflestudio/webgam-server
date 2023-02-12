package com.wafflestudio.webgam.domain.event.exception

import com.wafflestudio.webgam.global.common.exception.ErrorType
import com.wafflestudio.webgam.global.common.exception.WebgamException

class ObjectEventNotFoundException(id: Long): WebgamException.NotFound(ErrorType.NotFound.OBJECT_EVENT_NOT_FOUND,
    "Event with id $id does not exists.")