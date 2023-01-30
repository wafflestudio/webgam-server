package com.wafflestudio.webgam.domain.`object`.model

import com.fasterxml.jackson.annotation.JsonProperty

enum class PageObjectType {
    @JsonProperty("DEFAULT")
    DEFAULT,
    @JsonProperty("TEXT")
    TEXT,
    @JsonProperty("IMAGE")
    IMAGE,
}