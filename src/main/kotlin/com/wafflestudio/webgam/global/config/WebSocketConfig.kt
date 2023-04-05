package com.wafflestudio.webgam.global.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer


@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig (
        private val stompHandler: StompHandler
        ): WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/topic", "/queue", "/project")
        config.setApplicationDestinationPrefixes("/app")
    }


    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:8080",
                        "http://webgam-dev.s3-website.ap-northeast-2.amazonaws.com:3000",
                        "http://localhost:3000")
                .withSockJS()
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:8080",
                        "http://webgam-dev.s3-website.ap-northeast-2.amazonaws.com:3000",
                        "http://localhost:3000")

    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(stompHandler)
    }
}