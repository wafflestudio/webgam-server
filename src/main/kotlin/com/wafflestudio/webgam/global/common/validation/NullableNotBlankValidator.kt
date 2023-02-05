package com.wafflestudio.webgam.global.common.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class NullableNotBlankValidator: ConstraintValidator<NullableNotBlank, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        value ?.let {
            return value.isNotBlank()
        }
        return true
    }
}