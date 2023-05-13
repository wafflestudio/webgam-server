package com.wafflestudio.webgam.domain.`object`.controller

import com.wafflestudio.webgam.domain.`object`.dto.PageObjectDto
import com.wafflestudio.webgam.domain.`object`.service.PageObjectService
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
class PageObjectWebsocketController(
        private val jwtProvider: JwtProvider,
        private val pageObjectService: PageObjectService,
) {

    private val logger: Logger = LoggerFactory.getLogger(PageObjectWebsocketController::class.java)

    @MessageMapping("/project/{projectId}/create.object")
    @SendTo("/project/{projectId}")
    fun createObject(
            @Payload request: PageObjectDto.CreateRequest,
            @CurrentUser user: User,
            @DestinationVariable projectId: Long
    ): WebSocketDto<PageObjectDto.SimpleResponse> {
        logger.info("Controller create object")
        val response = pageObjectService.createObject(user.id, request)
        return WebSocketDto(user, response)
    }

    @MessageMapping("/project/{projectId}/patch.object/{objectId}")
    @SendTo("/project/{projectId}")
    fun editObject(
            @Payload request: PageObjectDto.PatchRequest,
            @CurrentUser user: User,
            @DestinationVariable projectId: Long,
            @DestinationVariable objectId: Long
    ): WebSocketDto<PageObjectDto.DetailedResponse> {
        logger.info("Controller edit object")
        val response = pageObjectService.modifyObject(user.id, objectId, request)
        return WebSocketDto(user, response)
    }

    @MessageMapping("/project/{projectId}/delete.object/{objectId}")
    @SendTo("/project/{projectId}")
    fun deleteObject(
            @CurrentUser user: User,
            @DestinationVariable projectId: Long,
            @DestinationVariable objectId: Long
    ) {
        logger.info("Controller delete object")
        pageObjectService.deleteObject(user.id, objectId)

    }
}