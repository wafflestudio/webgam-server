package com.wafflestudio.webgam.global.security.controller

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.wafflestudio.webgam.global.common.dto.ErrorResponse
import com.wafflestudio.webgam.global.common.exception.WebgamException
import com.wafflestudio.webgam.global.security.exception.UnauthorizedException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class WebgamAuthenticationEntryPoint: AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException?
    ) {
        val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
        response.contentType = "application/json"
        response.characterEncoding = "utf-8"
        response.writer.write(
            gson.toJson(ErrorResponse(
            request.getAttribute("webgamException") as? WebgamException ?: UnauthorizedException()
        )))
        response.status = HttpServletResponse.SC_UNAUTHORIZED
    }
}