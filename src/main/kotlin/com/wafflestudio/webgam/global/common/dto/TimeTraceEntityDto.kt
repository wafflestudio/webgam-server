package com.wafflestudio.webgam.global.common.dto

import java.time.LocalDateTime

class TimeTraceEntityDto {
    open class Response(
        open val id: Long,
        open val createdAt: LocalDateTime,
        open val createdBy: String,
        open val modifiedAt: LocalDateTime,
        open val modifiedBy: String,
    )
}