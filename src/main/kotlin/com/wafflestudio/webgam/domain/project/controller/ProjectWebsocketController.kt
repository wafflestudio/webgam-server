package com.wafflestudio.webgam.domain.project.controller

import com.wafflestudio.webgam.domain.project.dto.ProjectDto
import com.wafflestudio.webgam.domain.project.service.ProjectService
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
class ProjectWebsocketController(
        private val jwtProvider: JwtProvider,
        private val projectService: ProjectService,
) {

    private val logger: Logger = LoggerFactory.getLogger(ProjectWebsocketController::class.java)

    @MessageMapping("/project/{projectId}/patch.project")
    @SendTo("/project/{projectId}")
    fun editProject(
            @Payload request: ProjectDto.PatchRequest,
            @CurrentUser user: User,
            @DestinationVariable projectId: Long)
            : WebSocketDto<ProjectDto.DetailedResponse> {
        logger.info("Controller edit project")

        logger.info("${user.id}")
        val response = projectService.patchProject(user.id, projectId, request)
        logger.info("$response")

        return WebSocketDto(user, response)
    }
}