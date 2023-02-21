package com.wafflestudio.webgam.global.websocket.listener

import com.wafflestudio.webgam.global.websocket.dto.WebSocketDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionConnectedEvent
import org.springframework.web.socket.messaging.SessionConnectEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent

@Component
class WebSocketEventListener (
        private val messagingTemplate: SimpMessageSendingOperations?
        ){
    private val logger: Logger = LoggerFactory.getLogger(WebSocketEventListener::class.java)

    // TODO remove ?, null
    @EventListener
    fun connectListener(event: SessionConnectEvent?){
        if (event != null){
            logger.info("Received a new websocket connect event from " + event.user)
            logger.info("Info: " + event.message)
        }
    }

    @EventListener
    fun connectedListener(event: SessionConnectedEvent?) {
        if (event != null) {
            logger.info("Completed new websocket connection from " + event.user)
            logger.info("Info: " + event.message)
        }
    }

    @EventListener
    fun subscribeListener(event: SessionSubscribeEvent?) {
        if (event != null){
            logger.info("Received new subscription from " + event.user)
            logger.info("Info: " + event.message)
        }
    }

    @EventListener
    fun unsubscribeListener(event: SessionUnsubscribeEvent?){
        if (event != null){
            logger.info(event.user.toString() + "unsubscribed")
            logger.info("Info: " + event.message)
        }
    }


    @EventListener
    fun disconnectListener(event: SessionDisconnectEvent?){
        if (event != null){
            // TODO add source user
            logger.info("Disconnected")
        }
    }
}