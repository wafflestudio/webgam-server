package com.wafflestudio.webgam.domain.`object`.exception

import com.wafflestudio.webgam.global.common.exception.ErrorType.Forbidden.NON_ACCESSIBLE_PAGE_OBJECT
import com.wafflestudio.webgam.global.common.exception.WebgamException

class NonAccessiblePageObjectException(id: Long): WebgamException.Forbidden(NON_ACCESSIBLE_PAGE_OBJECT,
    "You have no access to Object with id $id")