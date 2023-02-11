package com.wafflestudio.webgam.domain.`object`.exception

import com.wafflestudio.webgam.global.common.exception.ErrorType.NotFound.PAGE_OBJECT_NOT_FOUND
import com.wafflestudio.webgam.global.common.exception.WebgamException

class PageObjectNotFoundException(id: Long): WebgamException.NotFound(PAGE_OBJECT_NOT_FOUND,
    "Object with id $id does not exists.")