package com.wafflestudio.webgam.domain.user.model

import com.wafflestudio.webgam.domain.project.model.Project
import com.wafflestudio.webgam.global.common.model.BaseTimeTraceLazyDeletedEntity
import com.wafflestudio.webgam.global.common.model.WebgamAccessModel
import com.wafflestudio.webgam.global.security.dto.AuthDto
import jakarta.persistence.*

@Entity
@Table(name = "user")
class User(
    @Column(unique = true)
    val userId: String,

    var username: String,

    @Column(unique = true)
    var email: String,

    var password: String,

    /* From Here: Not saved in DB */

    @OneToMany(mappedBy = "owner", orphanRemoval = true, cascade = [CascadeType.ALL])
    val projects: MutableList<Project> = mutableListOf(),
): BaseTimeTraceLazyDeletedEntity(), WebgamAccessModel {
    constructor(signupRequest: AuthDto.SignupRequest, encryptedPassword: String): this(
        userId = signupRequest.userId!!,
        username = signupRequest.username!!,
        email = signupRequest.email!!,
        password = encryptedPassword,
        projects = mutableListOf(),
    )

    override fun isAccessibleTo(currentUserId: Long): Boolean {
        return (this.id == currentUserId)
    }

    override fun delete() {
        isDeleted = true
        projects.forEach { it.delete() }
    }
}