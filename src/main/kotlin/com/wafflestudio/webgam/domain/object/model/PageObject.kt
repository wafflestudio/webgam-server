package com.wafflestudio.webgam.domain.`object`.model

import com.wafflestudio.webgam.domain.event.model.ObjectEvent
import com.wafflestudio.webgam.domain.`object`.dto.PageObjectDto
import com.wafflestudio.webgam.domain.page.model.ProjectPage
import com.wafflestudio.webgam.global.common.model.BaseTimeTraceLazyDeletedEntity
import com.wafflestudio.webgam.global.common.model.WebgamAccessModel
import jakarta.persistence.*
import java.time.LocalDateTime

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

    var opacity: Int,

    var textContent: String?,

    var fontSize: Int?,

    var lineHeight: Int?,

    var letterSpacing: Int?,

    var backgroundColor: String?,

    var strokeWidth: Int?,

    var strokeColor: String?,

    var imageSource: String?,

    var isReversed: Boolean?,

    var rotateDegree: Int?,

    /* From Here: Not saved in DB */

    @OneToMany(mappedBy = "object", orphanRemoval = true, cascade = [CascadeType.ALL])
    val events: MutableList<ObjectEvent> = mutableListOf(),

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
        opacity = createRequest.opacity!!,
        textContent = createRequest.textContent,
        fontSize = createRequest.fontSize,
        imageSource = createRequest.imageSource,
        lineHeight = createRequest.lineHeight,
        letterSpacing = createRequest.letterSpacing,
        backgroundColor = createRequest.backgroundColor,
        strokeWidth = createRequest.strokeWidth,
        strokeColor = createRequest.strokeColor,
        isReversed = createRequest.isReversed,
        rotateDegree = createRequest.rotateDegree,
    ) {
        page.objects.add(this)
    }

    @get:Transient
    val event: ObjectEvent?
        get() = events.firstOrNull { !it.isDeleted }

    override fun delete() {
        isDeleted = true
        deletedAt = LocalDateTime.now()
        event?.delete()
    }
}