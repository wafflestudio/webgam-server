package com.wafflestudio.webgam.global.websocket.listener

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionConnectedEvent
import org.springframework.web.socket.messaging.SessionConnectEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent

@Component
class WebSocketEventListener (
        private val messagingTemplate: SimpMessageSendingOperations?
        ){
    private val logger: Logger = LoggerFactory.getLogger(WebSocketEventListener::class.java)

    @EventListener(SessionConnectEvent::class)
    fun connectListener(event: SessionConnectEvent){
        logger.info("Received a new websocket connect event from " + event.user)
        logger.info("Info: " + event.message)
    }

    @EventListener(SessionConnectedEvent::class)
    fun connectedListener(event: SessionConnectedEvent) {
        logger.info("Completed new websocket connection from " + event.user)
        logger.info("Info: " + event.message)
    }

    @EventListener(SessionSubscribeEvent::class)
    fun subscribeListener(event: SessionSubscribeEvent) {
        logger.info("Received new subscription from " + event.user)
        logger.info("Info: " + event.message)
    }

    @EventListener(SessionUnsubscribeEvent::class)
    fun unsubscribeListener(event: SessionUnsubscribeEvent){
        logger.info(event.user.toString() + "unsubscribed")
        logger.info("Info: " + event.message)
    }


    @EventListener(SessionDisconnectEvent::class)
    fun disconnectListener(event: SessionDisconnectEvent){
        logger.info(event.user.toString() + "Disconnected")
    }
}