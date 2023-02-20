package com.wafflestudio.webgam.global.websocket.dto

import jakarta.validation.constraints.NotBlank

class WebSocketDto {
    data class Message(
            @NotBlank
            var content: String?,
            @NotBlank
            var type: String,
    )

    data class BroadcastMessage(
            var senderId: Long?,
            @NotBlank
            var content: String?,
            @NotBlank
            var type: String
    )
}