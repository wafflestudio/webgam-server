package com.wafflestudio.webgam.global.websocket.controller

import com.wafflestudio.webgam.global.websocket.dto.WebSocketDto.ChatMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping


@Validated
@Controller
class WebSocketController (
        private val template: SimpMessagingTemplate
        ){

    private val logger: Logger = LoggerFactory.getLogger(WebSocketController::class.java)

    @MessageMapping("/test")
    @SendTo("/topic/public")
    fun test(){
        logger.info("Websocket Controller MessageMapping test")
    }

    @MessageMapping("/send")
    @SendTo("/topic/public")
    fun sendMessage(message: Message<ChatMessage>): ChatMessage
    {
        logger.info("Controller /app/send")
        logger.info("Message: ${message.payload.content}")
        val sentMessage = message.payload
        return sentMessage
    }

    // TODO check if this works
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    fun handleException(exception: Throwable): String? {
        return exception.message
    }

    @GetMapping("websocket")
    fun getRoom(model: Model): String {
        logger.info("Controller /websocket")
        return "websocket"
    }
}