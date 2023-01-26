package com.wafflestudio.webgam.global.security

import com.wafflestudio.webgam.global.security.model.UserPrincipal
import org.springframework.data.domain.AuditorAware
import org.springframework.security.core.context.SecurityContextHolder
import java.util.*

class AuditorAwareImpl(): AuditorAware<String> {
    override fun getCurrentAuditor(): Optional<String> {
        return when (val userPrincipal = SecurityContextHolder.getContext().authentication?.principal) {
            is UserPrincipal -> Optional.of(userPrincipal.getAuditorSignature())
            else -> Optional.empty()
        }
    }
}