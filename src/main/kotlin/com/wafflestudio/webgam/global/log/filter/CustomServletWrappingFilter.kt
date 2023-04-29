package com.wafflestudio.webgam.global.log.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.util.*

@Component
class CustomServletWrappingFilter: OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        // Create wrapping request, response for multiple read
        val wrappingRequest = ContentCachingRequestWrapper(request)
        val wrappingResponse = ContentCachingResponseWrapper(response)

        // Create traceId for logging
        val traceId = UUID.randomUUID()
        wrappingRequest.setAttribute("traceId", traceId)


        filterChain.doFilter(wrappingRequest, wrappingResponse)

//         Copy response body to actual response
        wrappingResponse.copyBodyToResponse()
    }
}