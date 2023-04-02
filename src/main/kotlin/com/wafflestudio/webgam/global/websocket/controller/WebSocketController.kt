package com.wafflestudio.webgam.global.websocket.controller

import com.wafflestudio.webgam.domain.event.dto.ObjectEventDto
import com.wafflestudio.webgam.domain.event.service.ObjectEventService
import com.wafflestudio.webgam.domain.`object`.dto.PageObjectDto
import com.wafflestudio.webgam.domain.`object`.service.PageObjectService
import com.wafflestudio.webgam.domain.page.dto.ProjectPageDto
import com.wafflestudio.webgam.domain.page.service.ProjectPageService
import com.wafflestudio.webgam.domain.project.dto.ProjectDto
import com.wafflestudio.webgam.domain.project.service.ProjectService
import com.wafflestudio.webgam.global.security.jwt.JwtProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.*
import org.springframework.messaging.simp.annotation.SendToUser
import com.wafflestudio.webgam.global.security.model.UserPrincipal
import com.wafflestudio.webgam.global.websocket.dto.WebSocketDto
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated


@Validated
@Controller
class WebSocketController (
        private val jwtProvider: JwtProvider,
        private val projectService: ProjectService,
        private val projectPageService: ProjectPageService,
        private val pageObjectService: PageObjectService,
        private val objectEventService: ObjectEventService
        ){

    private val logger: Logger = LoggerFactory.getLogger(WebSocketController::class.java)

    @MessageMapping("/test")
    @SendTo("/topic/public")
    fun test(){
        logger.info("Websocket Controller MessageMapping test")
    }

    @MessageMapping("/send")
    @SendTo("/topic/public")
    fun sendMessage(@Payload message: WebSocketDto<String>, @Header("Authorization") token: String): WebSocketDto<String>
    {
        logger.info("Controller /app/send")
        logger.info("Message: ${message.content}")

        logger.info(token)
        val auth = jwtProvider.getAuthenticationFromToken(token)
        logger.info("User details: $auth")

        val sentMessage = WebSocketDto<String>((auth.principal as UserPrincipal).user, message.content)
        return sentMessage
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    fun handleException(exception: Throwable, userPrincipal: UserPrincipal): WebSocketDto<String?>{
        logger.info("Controller /queue/errors")
        logger.info(exception.message)
        return WebSocketDto<String?>(userPrincipal.user, exception.message)
    }
    
    @MessageMapping("/project/{projectId}/patch.project")
    @SendTo("/project/{projectId}")
    fun editProject(@Payload request: ProjectDto.PatchRequest, @Header("Authorization") token: String, @DestinationVariable projectId: Long)
    : WebSocketDto<ProjectDto.DetailedResponse> {
        logger.info("Controller edit project")

        val auth = jwtProvider.getAuthenticationFromToken(token)
        val user = (auth.principal as UserPrincipal).user
        logger.info("${user.id}")
        val response = projectService.patchProject(user.id, projectId, request)
        logger.info("$response")

        return WebSocketDto(user, response)
    }

    @MessageMapping("/project/{projectId}/create.page")
    @SendTo("/project/{projectId}")
    fun createPage(
            @Payload request: ProjectPageDto.CreateRequest,
            @Header("Authorization") token: String,
            @DestinationVariable projectId: Long)
    : WebSocketDto<ProjectPageDto.DetailedResponse> {
        logger.info("Controller create page")

        val auth = jwtProvider.getAuthenticationFromToken(token)
        val user = (auth.principal as UserPrincipal).user
        val revisedRequest = ProjectPageDto.CreateRequest(projectId, request.name)
        val response = projectPageService.createProjectPage(user.id, revisedRequest)

        return WebSocketDto(user, response)

    }

    @MessageMapping("/project/{projectId}/patch.page/{pageId}")
    @SendTo("/project/{projectId}")
    fun editPage(@Payload request: ProjectPageDto.PatchRequest,
                 @Header("Authorization") token: String,
                 @DestinationVariable projectId: Long,
                 @DestinationVariable pageId: Long)
    : WebSocketDto<ProjectPageDto.DetailedResponse> {
        logger.info("Controller edit page")

        val auth = jwtProvider.getAuthenticationFromToken(token)
        val user = (auth.principal as UserPrincipal).user

        val response = projectPageService.patchProjectPage(user.id, pageId, request)

        return WebSocketDto(user, response)

    }

    @MessageMapping("/project/{projectId}/delete.page/{pageId}")
    @SendTo("/project/{projectId}")
    fun deletePage(
                   @Header("Authorization") token: String,
                   @DestinationVariable projectId: Long,
                   @DestinationVariable pageId: Long)
    {
        logger.info("Controller delete page")

        val auth = jwtProvider.getAuthenticationFromToken(token)

        val myId = (auth.principal as UserPrincipal).getUserId()
        projectPageService.deleteProjectPage(myId, pageId)

    }

    @MessageMapping("/project/{projectId}/create.object")
    @SendTo("/project/{projectId}")
    fun createObject(
            @Payload request: PageObjectDto.CreateRequest,
            @Header("Authorization") token: String,
            @DestinationVariable projectId: Long
    ): WebSocketDto<PageObjectDto.SimpleResponse> {
        logger.info("Controller create object")

        val auth = jwtProvider.getAuthenticationFromToken(token)
        val user = (auth.principal as UserPrincipal).user
        val response = pageObjectService.createObject(user.id, request)

        return WebSocketDto(user, response)
    }

    @MessageMapping("/project/{projectId}/patch.object/{objectId}")
    @SendTo("/project/{projectId}")
    fun editObject(
            @Payload request: PageObjectDto.PatchRequest,
            @Header("Authorization") token: String,
            @DestinationVariable projectId: Long,
            @DestinationVariable objectId: Long
    ): WebSocketDto<PageObjectDto.DetailedResponse> {
        logger.info("Controller edit object")

        val auth = jwtProvider.getAuthenticationFromToken(token)
        val user = (auth.principal as UserPrincipal).user
        val response = pageObjectService.modifyObject(user.id, objectId, request)

        return WebSocketDto(user, response)
    }

    @MessageMapping("/project/{projectId}/delete.object/{objectId}")
    @SendTo("/project/{projectId}")
    fun deleteObject(
            @Header("Authorization") token: String,
            @DestinationVariable projectId: Long,
            @DestinationVariable objectId: Long
    ) {
        logger.info("Controller delete object")

        val auth = jwtProvider.getAuthenticationFromToken(token)

        val myId = (auth.principal as UserPrincipal).getUserId()
        pageObjectService.deleteObject(myId, objectId)

    }

    @MessageMapping("/project/{projectId}/create.event")
    @SendTo("/project/{projectId}")
    fun createEvent(
            @Payload request: ObjectEventDto.CreateRequest,
            @Header("Authorization") token: String,
            @DestinationVariable projectId: Long
    ): WebSocketDto<ObjectEventDto.SimpleResponse> {
        logger.info("Controller create event")
        val auth = jwtProvider.getAuthenticationFromToken(token)
        val user = (auth.principal as UserPrincipal).user
        val response = objectEventService.createEvent(user.id, request)
        return WebSocketDto(user, response)

    }

    @MessageMapping("/project/{projectId}/patch.event/{eventId}")
    @SendTo("/project/{projectId}")
    fun patchEvent(
            @Payload request: ObjectEventDto.PatchRequest,
            @Header("Authorization") token: String,
            @DestinationVariable projectId: Long,
            @DestinationVariable eventId: Long
    ): WebSocketDto<ObjectEventDto.SimpleResponse> {
        logger.info("Controller create event")
        val auth = jwtProvider.getAuthenticationFromToken(token)
        val user = (auth.principal as UserPrincipal).user

        val response = objectEventService.updateEvent(user.id, eventId, request)
        return WebSocketDto(user, response)

    }

    @MessageMapping("/project/{projectId}/delete.event/{eventId}")
    @SendTo("/project/{projectId}")
    fun deleteEvent(
            @Header("Authorization") token: String,
            @DestinationVariable projectId: Long,
            @DestinationVariable eventId: Long
    ){
        logger.info("Controller create event")
        val auth = jwtProvider.getAuthenticationFromToken(token)
        val myId = (auth.principal as UserPrincipal).getUserId()

        objectEventService.deleteEvent(myId, eventId)
    }


}