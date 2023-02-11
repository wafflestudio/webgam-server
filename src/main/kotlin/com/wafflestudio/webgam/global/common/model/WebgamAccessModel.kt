package com.wafflestudio.webgam.global.common.model

interface WebgamAccessModel {
    fun isAccessibleTo(currentUserId: Long): Boolean
    // TODO: isAccessibleTo -> isReadableTo, isWritableTo
}