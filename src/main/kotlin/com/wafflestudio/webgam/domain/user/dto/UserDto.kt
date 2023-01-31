package com.wafflestudio.webgam.domain.user.dto

import com.wafflestudio.webgam.domain.user.model.User
import com.wafflestudio.webgam.global.common.validation.NullableNotBlank
import jakarta.validation.constraints.Email

class UserDto {
    data class PatchRequest(
        @field:NullableNotBlank
        val username: String?,
        @field:Email
        val email: String?,
    )

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