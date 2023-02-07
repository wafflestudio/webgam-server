package com.wafflestudio.webgam.domain.page.exception

import com.wafflestudio.webgam.global.common.exception.ErrorType
import com.wafflestudio.webgam.global.common.exception.WebgamException

class NonAccessibleProjectPageException(id: Long, name: String): WebgamException.Forbidden(
        ErrorType.Forbidden.NON_ACCESSIBLE_PROJECT_PAGE,
                "You have no access to ProjectPage with Project id $id and Page name $name"
)