package com.wafflestudio.webgam.domain.page.dto

import com.wafflestudio.webgam.domain.`object`.dto.PageObjectDto
import com.wafflestudio.webgam.domain.page.model.ProjectPage
import com.wafflestudio.webgam.global.common.dto.TimeTraceEntityDto
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

class ProjectPageDto {
    data class CreateRequest(
        @field:NotNull
        val projectId: Long,
        @field:NotBlank
        val name: String?,
    )

    data class PatchRequest(
            @field:NotBlank
            val name: String?,
    )

    data class SimpleResponse(
        override val id: Long,
        override val createdAt: LocalDateTime,
        override val createdBy: String,
        override val modifiedAt: LocalDateTime,
        override val modifiedBy: String,
        val name: String,
    ): TimeTraceEntityDto.Response(id, createdAt, createdBy, modifiedAt, modifiedBy) {
        constructor(projectPage: ProjectPage): this(
            id = projectPage.id,
            createdAt = projectPage.createdAt,
            createdBy = projectPage.createdBy,
            modifiedAt = projectPage.modifiedAt,
            modifiedBy = projectPage.modifiedBy,
            name = projectPage.name,
        )
    }

    data class DetailedResponse(
        override val id: Long,
        override val createdAt: LocalDateTime,
        override val createdBy: String,
        override val modifiedAt: LocalDateTime,
        override val modifiedBy: String,
        val name: String,
        /* Detailed */
        val projectId: Long,
        val objects: List<PageObjectDto.DetailedResponse>
    ): TimeTraceEntityDto.Response(id, createdAt, createdBy, modifiedAt, modifiedBy) {
        constructor(projectPage: ProjectPage): this(
            id = projectPage.id,
            createdAt = projectPage.createdAt,
            createdBy = projectPage.createdBy,
            modifiedAt = projectPage.modifiedAt,
            modifiedBy = projectPage.modifiedBy,
            name = projectPage.name,
            /* Detailed */
            projectId = projectPage.project.id,
            objects = projectPage.objects.map { PageObjectDto.DetailedResponse(it) }
        )
    }
}