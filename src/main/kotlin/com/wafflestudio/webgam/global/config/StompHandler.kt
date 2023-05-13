package com.wafflestudio.webgam.global.config

import com.wafflestudio.webgam.global.security.exception.InvalidJwtException
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component
import com.wafflestudio.webgam.global.security.jwt.JwtProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.messaging.Message

@Component
class StompHandler(
        private val tokenProvider: JwtProvider
) : ChannelInterceptor {
    private val logger: Logger = LoggerFactory.getLogger(StompHandler::class.java)

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = StompHeaderAccessor.wrap(message)

        if (accessor.command == StompCommand.CONNECT) {
            val token = accessor.getFirstNativeHeader("Authorization") ?: throw InvalidJwtException("")
            tokenProvider.validate(token)
            val authentication = tokenProvider.getAuthenticationFromToken(token)
            logger.info("authentication: $authentication")
            accessor.user = authentication

        }

        return message
    }


}
