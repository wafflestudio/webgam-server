package com.wafflestudio.webgam.global.log.filter

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.webgam.global.common.dto.ErrorResponse
import com.wafflestudio.webgam.global.common.exception.ErrorType
import com.wafflestudio.webgam.global.common.exception.WebgamException
import com.wafflestudio.webgam.global.log.Logger
import com.wafflestudio.webgam.global.log.dto.RequestResponseLog
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.time.LocalDateTime
import java.util.*
import kotlin.time.Duration
import kotlin.time.DurationUnit

class LogFilter(
        private val objectMapper: ObjectMapper,
        private val activateProfile: String,
): OncePerRequestFilter() {

    private val log = Logger.logger

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        // Create wrapping request, response for multiple read
        val wrappingRequest = ContentCachingRequestWrapper(request)
        val wrappingResponse = ContentCachingResponseWrapper(response)

        // Create traceId for logging
        val traceId = UUID.randomUUID()
        wrappingRequest.setAttribute("traceId", traceId)

        try {
            // Do Business Logic
            filterChain.doFilter(wrappingRequest, wrappingResponse)
            // Log result
            logRequestResponse(wrappingRequest, wrappingResponse)
        } catch (e: Exception) {
            // Set response
            wrappingResponse.contentType = MediaType.APPLICATION_JSON_VALUE
            wrappingResponse.response.writer.use {
                it.println(objectMapper.writeValueAsString(
                        ErrorResponse(
                                object: WebgamException.ServerError(
                                        ErrorType.ServerError.DEFAULT,
                                        "Internal Server Error"
                                ) {}
                        )
                ))
            }

            // Log with stack trace
            logRequestResponse(wrappingRequest, wrappingResponse, e.stackTraceToString())

            // Print stack trace when activated profile is local
            if (activateProfile == "local") {
                e.printStackTrace()
            }
        }

        // Copy response body to actual response
        wrappingResponse.copyBodyToResponse()
    }

    private fun logRequestResponse(
            request: ContentCachingRequestWrapper,
            response: ContentCachingResponseWrapper,
            stackTrace: String? = null,
    ) {
        val traceId = request.getAttribute("traceId")!! as UUID
        val className = request.getAttribute("className")!! as String
        val methodName = request.getAttribute("methodName")!! as String
        val elapsedTime = request.getAttribute("elapsedTime") as Duration?

        val logContent = RequestResponseLog(
                traceId,
                className,
                httpMethod = request.method,
                uri = request.requestURI,
                method = methodName,
                params = getParameters(request),
                headers = getHeaders(request),
                logTime = LocalDateTime.now(),
                elapsedTime = elapsedTime?.toString(unit = DurationUnit.MILLISECONDS),
                requestBody = objectMapper.readTree(request.contentAsByteArray),
                responseBody = response.contentAsByteArray.decodeToString(),
                stackTrace = stackTrace,
        )

        when (stackTrace) {
            null -> log.info {objectMapper.writeValueAsString(logContent)}
            else -> log.error {objectMapper.writeValueAsString(logContent)}
        }
    }

    private fun getParameters(request: ContentCachingRequestWrapper): Map<String, String> {
        val paramsNames = request.parameterNames.toList()
        val replacedParamsNames = paramsNames.map {
            it.replace("\\.", "-")
        }
        return (replacedParamsNames zip paramsNames).associate {
            it.first to request.getParameter(it.second)
        }
    }

    private fun getHeaders(request: ContentCachingRequestWrapper): Map<String, String> {
        val headerNames = request.headerNames.toList()
        val replacedHeaderNames = headerNames.map {
            it.replace("\\.", "-")
        }
        return (replacedHeaderNames zip headerNames).associate {
            it.first to request.getHeader(it.second)
        }
    }

}