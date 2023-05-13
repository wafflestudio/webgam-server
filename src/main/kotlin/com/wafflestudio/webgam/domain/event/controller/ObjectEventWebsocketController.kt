package com.wafflestudio.webgam.domain.event.controller

import com.wafflestudio.webgam.domain.event.dto.ObjectEventDto
import com.wafflestudio.webgam.domain.event.service.ObjectEventService
import com.wafflestudio.webgam.domain.user.model.User
import com.wafflestudio.webgam.global.security.CurrentUser
import com.wafflestudio.webgam.global.security.jwt.JwtProvider
import com.wafflestudio.webgam.global.websocket.dto.WebSocketDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.*
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated

@Validated
@Controller
class ObjectEventWebsocketController(
        private val jwtProvider: JwtProvider,
        private val objectEventService: ObjectEventService,
) {

    private val logger: Logger = LoggerFactory.getLogger(ObjectEventWebsocketController::class.java)

    @MessageMapping("/project/{projectId}/create.event")
    @SendTo("/project/{projectId}")
    fun createEvent(
            @Payload request: ObjectEventDto.CreateRequest,
            @CurrentUser user: User,
            @DestinationVariable projectId: Long
    ): WebSocketDto<ObjectEventDto.SimpleResponse> {
        logger.info("Controller create event")
        val response = objectEventService.createEvent(user.id, request)
        return WebSocketDto(user, response)

    }

    @MessageMapping("/project/{projectId}/patch.event/{eventId}")
    @SendTo("/project/{projectId}")
    fun patchEvent(
            @Payload request: ObjectEventDto.PatchRequest,
            @CurrentUser user: User,
            @DestinationVariable projectId: Long,
            @DestinationVariable eventId: Long
    ): WebSocketDto<ObjectEventDto.SimpleResponse> {
        logger.info("Controller create event")
        val response = objectEventService.updateEvent(user.id, eventId, request)
        return WebSocketDto(user, response)

    }

    @MessageMapping("/project/{projectId}/delete.event/{eventId}")
    @SendTo("/project/{projectId}")
    fun deleteEvent(
            @CurrentUser user: User,
            @DestinationVariable projectId: Long,
            @DestinationVariable eventId: Long
    ){
        logger.info("Controller create event")
        objectEventService.deleteEvent(user.id, eventId)
    }

}