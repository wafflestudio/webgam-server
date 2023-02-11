package com.wafflestudio.webgam.domain.project.exception

import com.wafflestudio.webgam.global.common.exception.ErrorType
import com.wafflestudio.webgam.global.common.exception.WebgamException

class ProjectNotFoundException(id: Long): WebgamException.NotFound(ErrorType.NotFound.PROJECT_NOT_FOUND,
    "Project with id $id does not exists.")