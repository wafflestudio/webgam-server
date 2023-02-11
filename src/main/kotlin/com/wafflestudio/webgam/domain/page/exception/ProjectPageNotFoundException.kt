package com.wafflestudio.webgam.domain.page.exception

import com.wafflestudio.webgam.global.common.exception.ErrorType
import com.wafflestudio.webgam.global.common.exception.WebgamException

class ProjectPageNotFoundException(id: Long): WebgamException.NotFound(ErrorType.NotFound.PROJECT_PAGE_NOT_FOUND,
    "Page with id $id does not exists.")