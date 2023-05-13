package com.wafflestudio.webgam.global.websocket.dto

import com.wafflestudio.webgam.domain.user.dto.UserDto
import com.wafflestudio.webgam.domain.user.model.User

class WebSocketDto<T> (
        val sender: UserDto.SimpleResponse?,
        val content: T
        ){
        constructor(sender: User?, content: T): this(
                sender = if (sender != null) UserDto.SimpleResponse(sender) else null,
                content = content
        )

}