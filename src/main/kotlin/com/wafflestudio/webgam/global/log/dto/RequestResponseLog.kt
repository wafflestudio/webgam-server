package com.wafflestudio.webgam.global.log.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.util.*

data class RequestResponseLog(
        @JsonProperty
        val traceId: UUID,
        @JsonProperty
        val className: String? = null,
        @JsonProperty
        val httpMethod: String? = null,
        @JsonProperty
        val uri: String,
        @JsonProperty
        val method: String? = null,
        @JsonProperty
        val params: Map<String, Any>,
        @JsonProperty
        val headers: Map<String, Any>,
        @JsonProperty
        val logTime: LocalDateTime,
        @JsonProperty
        val elapsedTime: String? = null,
        @JsonProperty
        val requestBody: Any? = null,
        @JsonProperty
        val responseBody: Any? = null,
        @JsonProperty
        val stackTrace: String? = null,
)
