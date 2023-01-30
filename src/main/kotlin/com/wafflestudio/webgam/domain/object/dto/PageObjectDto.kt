package com.wafflestudio.webgam.domain.`object`.dto

import com.wafflestudio.webgam.domain.event.dto.ObjectEventDto
import com.wafflestudio.webgam.domain.`object`.model.PageObject
import com.wafflestudio.webgam.domain.`object`.model.PageObjectType
import com.wafflestudio.webgam.global.common.dto.TimeTraceEntityDto
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.hibernate.validator.constraints.URL
import java.time.LocalDateTime

class PageObjectDto {
    data class CreateRequest(
        @field:NotNull
        val pageId: Long?,
        @field:NotBlank
        val name: String?,
        val type: PageObjectType = PageObjectType.DEFAULT,
        @field:NotNull
        val width: Int?,
        @field:NotNull
        val height: Int?,
        @field:NotNull
        val xPosition: Int?,
        @field:NotNull
        val yPosition: Int?,
        @field:NotNull
        val zIndex: Int?,
        /* Optional */
        val textContent: String?,
        val fontSize: Int?,
        @field:URL
        val imageSource: String?,
    )

    data class SimpleResponse(
        override val id: Long,
        override val createdAt: LocalDateTime,
        override val createdBy: String,
        override val modifiedAt: LocalDateTime,
        override val modifiedBy: String,
        val name: String,
        val type: PageObjectType,
        val width: Int,
        val height: Int,
        val xPosition: Int,
        val yPosition: Int,
        val zIndex: Int,
        val textContent: String?,
        val fontSize: Int?,
        val imageSource: String?,
    ): TimeTraceEntityDto.Response(id, createdAt, createdBy, modifiedAt, modifiedBy) {
        constructor(pageObject: PageObject): this(
            id = pageObject.id,
            createdAt = pageObject.createdAt,
            createdBy = pageObject.createdBy,
            modifiedAt = pageObject.modifiedAt,
            modifiedBy = pageObject.modifiedBy,
            name = pageObject.name,
            type = pageObject.type,
            width = pageObject.width,
            height = pageObject.height,
            xPosition = pageObject.xPosition,
            yPosition = pageObject.yPosition,
            zIndex = pageObject.zIndex,
            textContent = pageObject.textContent,
            fontSize = pageObject.fontSize,
            imageSource = pageObject.imageSource,
        )
    }

    data class DetailedResponse(
        override val id: Long,
        override val createdAt: LocalDateTime,
        override val createdBy: String,
        override val modifiedAt: LocalDateTime,
        override val modifiedBy: String,
        val name: String,
        val type: PageObjectType,
        val width: Int,
        val height: Int,
        val xPosition: Int,
        val yPosition: Int,
        val zIndex: Int,
        val textContent: String?,
        val fontSize: Int?,
        val imageSource: String?,
        /* Detailed */
        val pageId: Long,
        val isInteractive: Boolean,
        val event: ObjectEventDto.SimpleResponse?,
    ): TimeTraceEntityDto.Response(id, createdAt, createdBy, modifiedAt, modifiedBy) {
        constructor(pageObject: PageObject): this(
            id = pageObject.id,
            createdAt = pageObject.createdAt,
            createdBy = pageObject.createdBy,
            modifiedAt = pageObject.modifiedAt,
            modifiedBy = pageObject.modifiedBy,
            name = pageObject.name,
            type = pageObject.type,
            width = pageObject.width,
            height = pageObject.height,
            xPosition = pageObject.xPosition,
            yPosition = pageObject.yPosition,
            zIndex = pageObject.zIndex,
            textContent = pageObject.textContent,
            fontSize = pageObject.fontSize,
            imageSource = pageObject.imageSource,
            /* Detailed */
            pageId = pageObject.page.id,
            isInteractive = pageObject.event != null,
            event = pageObject.event?.let { ObjectEventDto.SimpleResponse(it) }
        )
    }
}