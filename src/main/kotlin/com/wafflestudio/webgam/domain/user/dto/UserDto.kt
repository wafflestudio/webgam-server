package com.wafflestudio.webgam.domain.user.dto

import com.wafflestudio.webgam.domain.user.model.User

class UserDto {
    data class SimpleResponse(
        val userId: String,
        val username: String,
        val email: String,
    ) {
        constructor(user: User): this(
            userId = user.userId,
            username = user.username,
            email = user.email,
        )
    }
}