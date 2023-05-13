package com.wafflestudio.webgam.global.websocket.dto

import com.wafflestudio.webgam.domain.user.model.User
import com.wafflestudio.webgam.global.security.CurrentUser
import com.wafflestudio.webgam.global.security.jwt.JwtProvider
import com.wafflestudio.webgam.global.security.model.UserPrincipal
import org.springframework.core.MethodParameter
import org.springframework.messaging.Message
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver
import org.springframework.messaging.simp.stomp.StompHeaderAccessor

class UserArgumentResolver(
        private val jwtProvider: JwtProvider
): HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.parameterType == User::class.java
                && parameter.hasParameterAnnotation(CurrentUser::class.java)
    }

    override fun resolveArgument(parameter: MethodParameter, message: Message<*>): Any? {
        val accessor = StompHeaderAccessor.wrap(message)
        val token = accessor.getFirstNativeHeader("Authorization") as String
        val auth = jwtProvider.getAuthenticationFromToken(token)
        val user = (auth.principal as UserPrincipal).user
        return user
    }

}