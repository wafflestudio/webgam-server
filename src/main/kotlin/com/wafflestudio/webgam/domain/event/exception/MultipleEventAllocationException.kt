package com.wafflestudio.webgam.domain.event.exception

import com.wafflestudio.webgam.global.common.exception.ErrorType.Conflict.ONLY_SINGLE_EVENT_PER_OBJECT
import com.wafflestudio.webgam.global.common.exception.WebgamException

class MultipleEventAllocationException(id: Long): WebgamException.Conflict(ONLY_SINGLE_EVENT_PER_OBJECT,
    "Only 1 single event or none is allowed to be assigned per object." +
            " First, you need to delete the event for Object with id $id")