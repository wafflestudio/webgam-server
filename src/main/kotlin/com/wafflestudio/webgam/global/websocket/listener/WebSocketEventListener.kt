package com.wafflestudio.webgam.global.websocket.listener

import com.wafflestudio.webgam.global.websocket.dto.WebSocketDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionConnectedEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent

@Component
class WebSocketEventListener (
        private val messagingTemplate: SimpMessageSendingOperations?
        ){
    private val logger: Logger = LoggerFactory.getLogger(WebSocketEventListener::class.java)

    @EventListener
    fun connectListener(event: SessionConnectedEvent?) {
        logger.info("Received a new web socket connection")
    }

    @EventListener
    fun disconnectListener(event: SessionDisconnectEvent){
        val headerAccessor = StompHeaderAccessor.wrap(event.message)
        val senderId = headerAccessor.sessionAttributes!!["user_id"]
        if (senderId != null){
            logger.info("User Disconnected: $senderId")
            val message = WebSocketDto.BroadcastMessage(
                    senderId=null,
                    content="User Disconnected: $senderId",
                    type="DISCONNECT"
            )
            messagingTemplate!!.convertAndSend("topic/public", message)
        }
    }
}