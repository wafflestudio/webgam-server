package com.wafflestudio.webgam.global.security.jwt

import com.wafflestudio.webgam.global.common.exception.WebgamException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter

class JwtAuthenticationFilter(
    authenticationManager: AuthenticationManager?,
    private val jwtProvider: JwtProvider
) : BasicAuthenticationFilter(authenticationManager) {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        request.getHeader(JwtProvider.header) ?.let {
            try {
                jwtProvider.validate(it)
                SecurityContextHolder.getContext().authentication = jwtProvider.getAuthenticationFromToken(it)
            } catch (e: WebgamException) {
                request.setAttribute("webgamException", e)
            }
        }

        chain.doFilter(request, response)
    }
}