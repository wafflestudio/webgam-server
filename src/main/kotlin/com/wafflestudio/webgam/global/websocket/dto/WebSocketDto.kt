package com.wafflestudio.webgam.global.websocket.dto

import com.wafflestudio.webgam.global.common.dto.TimeTraceEntityDto
import jakarta.validation.constraints.NotBlank

class WebSocketDto {
    data class ChatMessage(
            // TODO add sender
            @NotBlank
            var content: String?
    )

}