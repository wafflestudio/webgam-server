package com.wafflestudio.webgam.global.security.controller

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.wafflestudio.webgam.global.common.exception.ErrorResponse
import com.wafflestudio.webgam.global.common.exception.WebgamException
import com.wafflestudio.webgam.global.security.exception.NoAccessException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

@Component
class WebgamAccessDeniedHandler: AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException?
    ) {
        val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
        response.contentType = "application/json"
        response.characterEncoding = "utf-8"
        response.writer.write(
            gson.toJson(ErrorResponse(
            request.getAttribute("webgamException") as? WebgamException ?: NoAccessException()
        )))
        response.status = HttpServletResponse.SC_FORBIDDEN
    }
}