package com.wafflestudio.webgam.domain.event.model

import com.wafflestudio.webgam.domain.event.dto.ObjectEventDto
import com.wafflestudio.webgam.domain.`object`.model.PageObject
import com.wafflestudio.webgam.domain.page.model.ProjectPage
import com.wafflestudio.webgam.global.common.model.BaseTimeTraceLazyDeletedEntity
import com.wafflestudio.webgam.global.common.model.WebgamAccessModel
import jakarta.persistence.*

@Entity
@Table(name = "object_event")
class ObjectEvent(
    @OneToOne(fetch = FetchType.LAZY)
    val `object`: PageObject,

    @ManyToOne(fetch = FetchType.LAZY)
    val nextPage: ProjectPage?,

    @Enumerated(EnumType.STRING)
    var transitionType: TransitionType,

    /* From Here: Not saved in DB */
): BaseTimeTraceLazyDeletedEntity(), WebgamAccessModel {
    override fun isAccessibleTo(currentUserId: Long): Boolean {
        return `object`.isAccessibleTo(currentUserId)
    }

    constructor(createRequest: ObjectEventDto.CreateRequest, `object`: PageObject, nextPage: ProjectPage? = null): this(
        `object` = `object`,
        nextPage = nextPage,
        transitionType = createRequest.transitionType,
    ) {
        `object`.event = this
        nextPage?.triggeredEvents?.add(this)
    }
}