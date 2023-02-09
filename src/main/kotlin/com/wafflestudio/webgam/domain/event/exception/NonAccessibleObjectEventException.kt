package com.wafflestudio.webgam.domain.event.exception

import com.wafflestudio.webgam.global.common.exception.ErrorType
import com.wafflestudio.webgam.global.common.exception.WebgamException

class NonAccessibleObjectEventException(id: Long): WebgamException.Forbidden(
    ErrorType.Forbidden.NON_ACCESSIBLE_OBJECT_EVENT,
    "You have no access to Event with id $id"
)