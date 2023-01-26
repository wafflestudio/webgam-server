package com.wafflestudio.webgam.domain.user.model

import com.wafflestudio.webgam.global.common.model.BaseTimeTraceLazyDeletedEntity
import com.wafflestudio.webgam.global.common.model.WebgamAccessModel
import com.wafflestudio.webgam.global.security.dto.AuthDto
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "user")
class User(
    @Column(unique = true)
    val userId: String,

    var username: String,

    @Column(unique = true)
    var email: String,

    var password: String,
): BaseTimeTraceLazyDeletedEntity(), WebgamAccessModel {
    constructor(signupRequest: AuthDto.SignupRequest, encryptedPassword: String): this(
        userId = signupRequest.userId!!,
        username = signupRequest.username!!,
        email = signupRequest.email!!,
        password = encryptedPassword,
    )

    override fun isAccessibleTo(currentUserId: Long): Boolean {
        return (this.id == currentUserId)
    }
}