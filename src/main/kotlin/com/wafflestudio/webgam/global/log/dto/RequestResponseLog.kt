package com.wafflestudio.webgam.global.log.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.util.*
import kotlin.time.Duration

data class RequestResponseLog(
        @JsonProperty
        val traceId: UUID,
        @JsonProperty
        val className: String,
        @JsonProperty
        val httpMethod: String,
        @JsonProperty
        val uri: String,
        @JsonProperty
        val method: String,
        @JsonProperty
        val params: Map<String, Any>,
        @JsonProperty
        val logTime: LocalDateTime,
        @JsonProperty
        val requestBody: Any? = null,
        @JsonProperty
        val responseBody: Any? = null,
        @JsonProperty
        val elapsedTime: Long? = null,
        @JsonProperty
        val stackTrace: String? = null,
)
