package com.wafflestudio.webgam.domain.event.dto

import com.wafflestudio.webgam.domain.event.model.ObjectEvent
import com.wafflestudio.webgam.domain.event.model.TransitionType
import com.wafflestudio.webgam.domain.event.model.TransitionType.DEFAULT
import com.wafflestudio.webgam.global.common.dto.TimeTraceEntityDto
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime

class ObjectEventDto {
    data class CreateRequest(
        @field:[NotNull Positive]
        val objectId: Long?,
        val transitionType: TransitionType = DEFAULT,
        /* Optional */
        @field:Positive
        val nextPageId: Long?,
    )

    data class PatchRequest(
        val transitionType: TransitionType?,
        @field:Positive
        val nextPageId: Long?,
    )

    data class SimpleResponse(
        override val id: Long,
        override val createdAt: LocalDateTime,
        override val createdBy: String,
        override val modifiedAt: LocalDateTime,
        override val modifiedBy: String,
        val transitionType: TransitionType,
    ): TimeTraceEntityDto.Response(id, createdAt, createdBy, modifiedAt, modifiedBy) {
        constructor(objectEvent: ObjectEvent): this(
            id = objectEvent.id,
            createdAt = objectEvent.createdAt,
            createdBy = objectEvent.createdBy,
            modifiedAt = objectEvent.modifiedAt,
            modifiedBy = objectEvent.modifiedBy,
            transitionType = objectEvent.transitionType,
        )
    }
}