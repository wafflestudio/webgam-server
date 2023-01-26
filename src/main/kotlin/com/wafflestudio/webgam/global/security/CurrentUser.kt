package com.wafflestudio.webgam.global.security

import org.springframework.security.core.annotation.AuthenticationPrincipal
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

@Target(VALUE_PARAMETER)
@Retention(RUNTIME)
@AuthenticationPrincipal(expression="userId")
annotation class CurrentUser(val required: Boolean = true)
