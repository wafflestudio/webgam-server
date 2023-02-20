package com.wafflestudio.webgam.global.websocket.controller

import com.wafflestudio.webgam.global.security.CurrentUser
import com.wafflestudio.webgam.global.websocket.dto.WebSocketDto.BroadcastMessage
import com.wafflestudio.webgam.global.websocket.dto.WebSocketDto.Message
import com.wafflestudio.webgam.global.websocket.listener.WebSocketEventListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController


@Validated
@Controller
class WebSocketController (
        private val template: SimpMessagingTemplate
        ){

    private val logger: Logger = LoggerFactory.getLogger(WebSocketController::class.java)

    @MessageMapping("/send")
    fun sendMessage(@CurrentUser myId: Long, @Payload message: Message)
    {
        logger.info("Controller /app/send")
        logger.info("Message: ${message.content}, ${message.type}")
        val sentMessage = BroadcastMessage(senderId = myId, content = message.content, type="SEND")
        template.convertAndSend("topic/public", sentMessage)
    }

    @SubscribeMapping("/subscribe")
    @SendTo("/topic/public")
    fun addUser(@CurrentUser myId: Long,
                @Payload message: Message,
                headerAccessor: SimpMessageHeaderAccessor)
    : BroadcastMessage {
        headerAccessor.sessionAttributes!!["user_id"] = myId
        return BroadcastMessage(senderId = null, content="User $myId added to public", type="SUBSCRIBE")
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    fun handleException(exception: Throwable): String? {
        return exception.message
    }

    @GetMapping("websocket")
    fun getRoom(model: Model): String {
        logger.info("Controller /websocket")
        model.addAttribute("room.name", "chat")
        return "websocket"
    }

}