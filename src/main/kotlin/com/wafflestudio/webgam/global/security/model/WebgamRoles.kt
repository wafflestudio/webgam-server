package com.wafflestudio.webgam.global.security.model

enum class WebgamRoles(val string: String) {
    USER("USER"),
    DEV("DEV"),
    ADMIN("ADMIN"),
    ;

    companion object {
        fun from(role: Any?): WebgamRoles? {
            role.let { return values().find { it.string == role } }
        }
    }

    override fun toString(): String {
        return string
    }
}