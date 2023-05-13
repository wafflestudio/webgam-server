package com.wafflestudio.webgam.global.websocket.controller

import com.wafflestudio.webgam.global.security.model.UserPrincipal
import com.wafflestudio.webgam.global.websocket.dto.WebSocketDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated

@Validated
@Controller
class ExceptionHandler {

    private val logger: Logger = LoggerFactory.getLogger(ExceptionHandler::class.java)

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    fun handleException(exception: Throwable, userPrincipal: UserPrincipal): WebSocketDto<String?> {
        logger.info("Controller /queue/errors")
        logger.info(exception.message)
        return WebSocketDto<String?>(userPrincipal.user, exception.message)
    }

}