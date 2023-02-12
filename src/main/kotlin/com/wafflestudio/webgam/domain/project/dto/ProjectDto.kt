package com.wafflestudio.webgam.domain.project.dto

import com.wafflestudio.webgam.domain.page.dto.ProjectPageDto
import com.wafflestudio.webgam.domain.project.model.Project
import com.wafflestudio.webgam.domain.user.dto.UserDto
import com.wafflestudio.webgam.global.common.dto.TimeTraceEntityDto
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

class ProjectDto {
    data class CreateRequest(
        @field:NotBlank
        val title: String?,
    )

    data class PatchRequest(
        @field:NotBlank
        val title: String?,
    )

    data class SimpleResponse(
        override val id: Long,
        override val createdAt: LocalDateTime,
        override val createdBy: String,
        override val modifiedAt: LocalDateTime,
        override val modifiedBy: String,
        val title: String,
    ): TimeTraceEntityDto.Response(id, createdAt, createdBy, modifiedAt, modifiedBy) {
        constructor(project: Project): this(
            id = project.id,
            createdAt = project.createdAt,
            createdBy = project.createdBy,
            modifiedAt = project.modifiedAt,
            modifiedBy = project.modifiedBy,
            title = project.title,
        )
    }

    data class DetailedResponse(
        override val id: Long,
        override val createdAt: LocalDateTime,
        override val createdBy: String,
        override val modifiedAt: LocalDateTime,
        override val modifiedBy: String,
        val title: String,
        /* Detailed */
        val owner: UserDto.SimpleResponse,
        val pages: List<ProjectPageDto.DetailedResponse>,
    ): TimeTraceEntityDto.Response(id, createdAt, createdBy, modifiedAt, modifiedBy) {
        constructor(project: Project): this(
            id = project.id,
            createdAt = project.createdAt,
            createdBy = project.createdBy,
            modifiedAt = project.modifiedAt,
            modifiedBy = project.modifiedBy,
            title = project.title,
            /* Detailed */
            owner = UserDto.SimpleResponse(project.owner),
            pages = project.pages.map { ProjectPageDto.DetailedResponse(it) }
        )
    }
}