package com.wafflestudio.webgam.global.websocket.controller

import com.wafflestudio.webgam.domain.`object`.dto.PageObjectDto
import com.wafflestudio.webgam.domain.`object`.service.PageObjectService
import com.wafflestudio.webgam.domain.page.dto.ProjectPageDto
import com.wafflestudio.webgam.domain.page.service.ProjectPageService
import com.wafflestudio.webgam.domain.project.dto.ProjectDto
import com.wafflestudio.webgam.domain.project.service.ProjectService
import com.wafflestudio.webgam.global.security.jwt.JwtProvider
import com.wafflestudio.webgam.global.security.service.UserPrincipalDetailsService
import com.wafflestudio.webgam.global.websocket.dto.WebSocketDto
import com.wafflestudio.webgam.global.websocket.dto.WebSocketDto.ChatMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.*
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageHeaderAccessor
import com.wafflestudio.webgam.global.security.model.UserPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping


@Validated
@Controller
class WebSocketController (
        private val template: SimpMessagingTemplate,
        private val jwtProvider: JwtProvider,
        private val projectService: ProjectService,
        private val projectPageService: ProjectPageService,
        private val pageObjectService: PageObjectService
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

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    fun handleException(exception: Throwable): String? {
        logger.info("Controller /queue/errors")
        return exception.message
    }
    
    @MessageMapping("/project/{projectId}/patch.project")
    @SendTo("/project/{projectId}")
    fun editProject(@Payload request: ProjectDto.PatchRequest, @Header("Authorization") token: String, @DestinationVariable projectId: Long)
    : ProjectDto.DetailedResponse {
        logger.info("Controller edit project")

        val auth = jwtProvider.getAuthenticationFromToken(token)

        val myId = (auth.principal as UserPrincipal).getUserId()
        val response = projectService.patchProject(myId, projectId, request)

        return response
    }

    @MessageMapping("/project/{projectId}/create.page")
    @SendTo("/project/{projectId}")
    fun createPage(@Payload request: ProjectPageDto.CreateRequest, @Header("Authorization") token: String, @DestinationVariable projectId: Long)
    : ProjectPageDto.DetailedResponse {
        logger.info("Controller create page")

        val auth = jwtProvider.getAuthenticationFromToken(token)

        val myId = (auth.principal as UserPrincipal).getUserId()
        val response = projectPageService.createProjectPage(myId, request)

        return response

    }

    @MessageMapping("/project/{projectId}/patch.page/{pageId}")
    @SendTo("/project/{projectId}")
    fun editPage(@Payload request: ProjectPageDto.PatchRequest,
                 @Header("Authorization") token: String,
                 @DestinationVariable projectId: Long,
                 @DestinationVariable pageId: Long)
            : ProjectPageDto.DetailedResponse {
        logger.info("Controller edit page")

        val auth = jwtProvider.getAuthenticationFromToken(token)

        val myId = (auth.principal as UserPrincipal).getUserId()
        val response = projectPageService.patchProjectPage(myId, pageId, request)

        return response

    }

    @MessageMapping("/project/{projectId}/delete.page/{pageId}")
    @SendTo("/project/{projectId}")
    fun deletePage(
                   @Header("Authorization") token: String,
                   @DestinationVariable projectId: Long,
                   @DestinationVariable pageId: Long)
            : ProjectPageDto.SimpleResponse {
        logger.info("Controller delete page")

        val auth = jwtProvider.getAuthenticationFromToken(token)

        val myId = (auth.principal as UserPrincipal).getUserId()
        val response = projectPageService.deleteProjectPage(myId, pageId)

        return response

    }

    @MessageMapping("/project/{projectId}/create.object/{objectId}")
    @SendTo("/project/{projectId}")
    fun createObject(
            @Payload request: PageObjectDto.CreateRequest,
            @Header("Authorization") token: String,
            @DestinationVariable projectId: Long,
            @DestinationVariable objectId: Long
    ): PageObjectDto.SimpleResponse {
        logger.info("Controller create object")

        val auth = jwtProvider.getAuthenticationFromToken(token)

        val myId = (auth.principal as UserPrincipal).getUserId()
        val response = pageObjectService.createObject(myId, request)

        return response
    }

    @MessageMapping("/project/{projectId}/patch.object/{objectId}")
    @SendTo("/project/{projectId}")
    fun editObject(
            @Payload request: PageObjectDto.PatchRequest,
            @Header("Authorization") token: String,
            @DestinationVariable projectId: Long,
            @DestinationVariable objectId: Long
    ): PageObjectDto.DetailedResponse {
        logger.info("Controller edit object")

        val auth = jwtProvider.getAuthenticationFromToken(token)

        val myId = (auth.principal as UserPrincipal).getUserId()
        val response = pageObjectService.modifyObject(myId, objectId, request)

        return response
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
        val response = pageObjectService.deleteObject(myId, objectId)

        return response
    }


}