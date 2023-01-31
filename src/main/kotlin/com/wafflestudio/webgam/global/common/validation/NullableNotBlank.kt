package com.wafflestudio.webgam.global.common.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.reflect.KClass

@Constraint(validatedBy = [NullableNotBlankValidator::class])
@Target(FIELD)
@Retention(RUNTIME)
annotation class NullableNotBlank(
    val message: String = "can be nullable but not blank",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
