package com.wafflestudio.webgam.global.websocket.controller

import com.wafflestudio.webgam.global.security.jwt.JwtProvider
import com.wafflestudio.webgam.global.websocket.dto.WebSocketDto.ChatMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.*
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping


@Validated
@Controller
class WebSocketController (
        private val template: SimpMessagingTemplate,
        private val jwtProvider: JwtProvider
        ){

    private val logger: Logger = LoggerFactory.getLogger(WebSocketController::class.java)

    @MessageMapping("/test")
    @SendTo("/topic/public")
    fun test(){
        logger.info("Websocket Controller MessageMapping test")
    }

    @MessageMapping("/send")
    @SendTo("/topic/public")
    fun sendMessage(@Payload message: ChatMessage, @Header("Authorization") token: String): ChatMessage
    {
        logger.info("Controller /app/send")
        logger.info("Message: ${message.content}")

        logger.info(token)
        val auth = jwtProvider.getAuthenticationFromToken(token)
        logger.info("User details: $auth")

        message.content = "From ${auth.name}: "+ message.content
        return message
    }

    // TODO check if this works
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    fun handleException(exception: Throwable): String? {
        logger.info("Controller /queue/errors")
        return exception.message
    }

}