package com.wafflestudio.webgam.domain.page.model

import com.wafflestudio.webgam.domain.event.model.ObjectEvent
import com.wafflestudio.webgam.domain.`object`.model.PageObject
import com.wafflestudio.webgam.domain.page.dto.ProjectPageDto
import com.wafflestudio.webgam.domain.project.model.Project
import com.wafflestudio.webgam.global.common.model.BaseTimeTraceLazyDeletedEntity
import com.wafflestudio.webgam.global.common.model.WebgamAccessModel
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "project_page")
class ProjectPage(
    @ManyToOne(fetch = FetchType.LAZY)
    val project: Project,

    var name: String,

    /* From Here: Not saved in DB */

    @OneToMany(mappedBy = "page", orphanRemoval = true, cascade = [CascadeType.ALL])
    val objects: MutableList<PageObject> = mutableListOf(),

    @OneToMany(mappedBy = "nextPage")
    val triggeredEvents: MutableList<ObjectEvent> = mutableListOf(),


    ): BaseTimeTraceLazyDeletedEntity(), WebgamAccessModel {
    override fun isAccessibleTo(currentUserId: Long): Boolean {
        return project.isAccessibleTo(currentUserId)
    }

    constructor(project: Project, createRequest: ProjectPageDto.CreateRequest): this(
        project = project,
        name = createRequest.name!!,
    ) {
        project.pages.add(this)
    }

    override fun delete() {
        isDeleted = true
        deletedAt = LocalDateTime.now()
        objects.forEach { it.delete() }
    }
}