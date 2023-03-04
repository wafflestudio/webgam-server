package com.wafflestudio.webgam.domain.event.exception

import com.wafflestudio.webgam.global.common.exception.ErrorType.BadRequest.PAGE_IN_OTHER_PROJECT
import com.wafflestudio.webgam.global.common.exception.WebgamException

class LinkNonRelatedPageException(id: Long): WebgamException.BadRequest(PAGE_IN_OTHER_PROJECT,
    "Page with id $id is in other project, you can not link this page.")