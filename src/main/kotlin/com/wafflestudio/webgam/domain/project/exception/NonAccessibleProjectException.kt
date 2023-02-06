package com.wafflestudio.webgam.domain.project.exception

import com.wafflestudio.webgam.global.common.exception.ErrorType
import com.wafflestudio.webgam.global.common.exception.WebgamException

class NonAccessibleProjectException(id: Long): WebgamException.Forbidden(ErrorType.Forbidden.NON_ACCESSIBLE_PROJECT,
    "You have no access to Project with id $id")