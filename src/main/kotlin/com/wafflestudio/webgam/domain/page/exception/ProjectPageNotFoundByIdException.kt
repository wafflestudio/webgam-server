package com.wafflestudio.webgam.domain.page.exception

import com.wafflestudio.webgam.global.common.exception.ErrorType
import com.wafflestudio.webgam.global.common.exception.WebgamException

class ProjectPageNotFoundByIdException(id: Long): WebgamException.NotFound(ErrorType.NotFound.PROJECT_PAGE_NOT_FOUND,
        "ProjectPage with id $id does not exist."
)
