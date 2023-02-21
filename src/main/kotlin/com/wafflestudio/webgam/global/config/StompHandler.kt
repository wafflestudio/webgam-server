package com.wafflestudio.webgam.global.config

import com.wafflestudio.webgam.global.security.exception.InvalidJwtException
import com.wafflestudio.webgam.global.security.exception.NoAccessException
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component
import com.wafflestudio.webgam.global.security.jwt.JwtProvider
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.messaging.Message

@Component
@Order(Ordered.HIGHEST_PRECEDENCE+99)
class StompHandler(
        private val tokenProvider: JwtProvider
) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = StompHeaderAccessor.wrap(message)

        // TODO uncomment to enable jwt auth
        /*if (accessor.command == StompCommand.CONNECT) {
            val token = accessor.getFirstNativeHeader("Authorization") ?: throw InvalidJwtException("")
            tokenProvider.validate(token)
        }*/
        return message
    }


}
