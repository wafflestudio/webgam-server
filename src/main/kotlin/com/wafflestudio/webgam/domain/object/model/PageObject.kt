package com.wafflestudio.webgam.domain.`object`.model

import com.wafflestudio.webgam.domain.event.model.ObjectEvent
import com.wafflestudio.webgam.domain.`object`.dto.PageObjectDto
import com.wafflestudio.webgam.domain.page.model.ProjectPage
import com.wafflestudio.webgam.global.common.model.BaseTimeTraceLazyDeletedEntity
import com.wafflestudio.webgam.global.common.model.WebgamAccessModel
import jakarta.persistence.*

@Entity
@Table(name = "page_object")
class PageObject(
    @ManyToOne(fetch = FetchType.LAZY)
    val page: ProjectPage,

    var name: String,

    @Enumerated(EnumType.STRING)
    var type: PageObjectType,

    var width: Int,

    var height: Int,

    var xPosition: Int,

    var yPosition: Int,

    var zIndex: Int,

    var textContent: String?,

    var fontSize: Int?,

    var imageSource: String?,

    @OneToOne(fetch = FetchType.LAZY)
    var event: ObjectEvent?,

    /* From Here: Not saved in DB */

    @OneToMany(mappedBy = "object", orphanRemoval = true, cascade = [CascadeType.ALL])
    val deletedEvents: MutableList<ObjectEvent> = mutableListOf(),

    ): BaseTimeTraceLazyDeletedEntity(), WebgamAccessModel {
    override fun isAccessibleTo(currentUserId: Long): Boolean {
        return page.isAccessibleTo(currentUserId)
    }

    constructor(page: ProjectPage, createRequest: PageObjectDto.CreateRequest): this(
        page = page,
        name = createRequest.name!!,
        type = createRequest.type,
        width = createRequest.width!!,
        height = createRequest.height!!,
        xPosition = createRequest.xPosition!!,
        yPosition = createRequest.yPosition!!,
        zIndex = createRequest.zIndex!!,
        textContent = createRequest.textContent,
        fontSize = createRequest.fontSize,
        imageSource = createRequest.imageSource,
        event = null
    ) {
        page.objects.add(this)
    }

    override fun delete() {
        isDeleted = true
        event?.delete()
    }
}