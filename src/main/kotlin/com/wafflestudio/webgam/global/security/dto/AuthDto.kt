package com.wafflestudio.webgam.global.security.dto

import com.wafflestudio.webgam.domain.user.dto.UserDto
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

class AuthDto {
    data class SignupRequest(
        @field:NotBlank
        val userId: String?,
        @field:NotBlank
        val username: String?,
        @field:Email @field:NotBlank
        val email: String?,
        @field:NotBlank
        val password: String?,
    )

    data class LoginRequest(
        @field:NotBlank
        val userId: String?,
        @field:NotBlank
        val password: String?,
    )

    data class Response(
        val user: UserDto.SimpleResponse,
        val message: String,
        val accessToken: String,
    )
}