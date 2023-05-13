package com.wafflestudio.webgam.domain.page.controller

import com.wafflestudio.webgam.domain.page.dto.ProjectPageDto
import com.wafflestudio.webgam.domain.page.service.ProjectPageService
import com.wafflestudio.webgam.global.security.jwt.JwtProvider
import com.wafflestudio.webgam.global.security.model.UserPrincipal
import com.wafflestudio.webgam.global.websocket.dto.WebSocketDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.*
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated

@Validated
@Controller
class ProjectPageWebsocketController(
        private val jwtProvider: JwtProvider,
        private val projectPageService: ProjectPageService,
) {

    private val logger: Logger = LoggerFactory.getLogger(ProjectPageWebsocketController::class.java)

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
}