package com.wafflestudio.webgam.global.websocket.dto

import jakarta.validation.constraints.NotBlank

class WebSocketDto {
    data class ChatMessage(
            // TODO add sender
            @NotBlank
            var content: String?
    )
}