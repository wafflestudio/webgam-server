package com.wafflestudio.webgam.global.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import org.springframework.messaging.simp.SimpMessageType.MESSAGE
import org.springframework.messaging.simp.SimpMessageType.SUBSCRIBE
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager

@Configuration
open class WebSocketSecurityConfig {
    fun messageAuthorizationManager(messages: MessageMatcherDelegatingAuthorizationManager.Builder): AuthorizationManager<Message<*>> {
        messages
                .nullDestMatcher().authenticated()
                .simpSubscribeDestMatchers("/user/**/queue/errors").permitAll()
                .simpDestMatchers("/app/**").hasRole("USER")
                .simpSubscribeDestMatchers("/user/**", "/topic/public", "/project/*").hasRole("USER")
                .simpTypeMatchers(MESSAGE, SUBSCRIBE).denyAll()
                .anyMessage().denyAll()

        return messages.build()
    }
}