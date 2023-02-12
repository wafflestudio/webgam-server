package com.wafflestudio.webgam.domain.project.model

import com.wafflestudio.webgam.domain.page.model.ProjectPage
import com.wafflestudio.webgam.domain.project.dto.ProjectDto
import com.wafflestudio.webgam.domain.user.model.User
import com.wafflestudio.webgam.global.common.model.BaseTimeTraceLazyDeletedEntity
import com.wafflestudio.webgam.global.common.model.WebgamAccessModel
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import org.hibernate.annotations.Type

@Entity
@Table(name = "project")
class Project(
    @ManyToOne(fetch = FetchType.LAZY)
    val owner: User,

    var title: String,

    @Type(JsonType::class)
    @Column(columnDefinition = "json")
    val variables: Map<String, String> = mapOf(),

    /* From Here: Not saved in DB */

    @OneToMany(mappedBy = "project", orphanRemoval = true, cascade = [CascadeType.ALL])
    val pages: MutableList<ProjectPage> = mutableListOf(),
): BaseTimeTraceLazyDeletedEntity(), WebgamAccessModel {
    override fun isAccessibleTo(currentUserId: Long): Boolean {
        return owner.id == currentUserId
    }

    constructor(user: User, createRequest: ProjectDto.CreateRequest): this(
            owner = user,
            title = createRequest.title!!,
    ){
        owner.projects.add(this)
    }

}