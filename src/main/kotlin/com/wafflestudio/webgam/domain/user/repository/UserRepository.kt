package com.wafflestudio.webgam.domain.user.repository;

import com.wafflestudio.webgam.domain.user.model.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository: JpaRepository<User, Long> {
    fun findByUserId(userId: String?): User?
    fun existsByUserIdOrEmail(userId: String, email: String): Boolean
}