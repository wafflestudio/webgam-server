package com.wafflestudio.webgam.global.websocket

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class LocalDateTimeDeserializer: JsonDeserializer<LocalDateTime> {

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalDateTime {

            val instant = Instant.ofEpochMilli(json.getAsJsonPrimitive().getAsLong())
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        }
}