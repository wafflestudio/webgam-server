package com.wafflestudio.webgam.global.security.service

import com.wafflestudio.webgam.domain.user.repository.UserRepository
import com.wafflestudio.webgam.global.security.exception.InvalidJwtException
import com.wafflestudio.webgam.global.security.model.UserPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class UserPrincipalDetailsService(
    private val userRepository: UserRepository
): UserDetailsService {
    override fun loadUserByUsername(userId: String?): UserDetails {
        val user = userRepository.findByUserId(userId) ?: throw InvalidJwtException("탈퇴하거나 존재하지 않는 회원입니다.")
        if (user.isDeleted) throw InvalidJwtException("탈퇴하거나 존재하지 않는 회원입니다.")
        return UserPrincipal(user)
    }
}