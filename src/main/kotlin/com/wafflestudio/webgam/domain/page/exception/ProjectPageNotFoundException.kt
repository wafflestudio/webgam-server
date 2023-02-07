package com.wafflestudio.webgam.domain.page.exception

import com.wafflestudio.webgam.global.common.exception.ErrorType
import com.wafflestudio.webgam.global.common.exception.WebgamException

class ProjectPageNotFoundException(id:Long, name:String): WebgamException.NotFound(ErrorType.NotFound.PROJECTPAGE_NOT_FOUND,
        "ProjectPage with id $id and name $name does not exist."
)