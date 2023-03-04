package com.wafflestudio.webgam.domain.event.service

import com.wafflestudio.webgam.domain.event.dto.ObjectEventDto.*
import com.wafflestudio.webgam.domain.event.exception.LinkNonRelatedPageException
import com.wafflestudio.webgam.domain.event.exception.MultipleEventAllocationException
import com.wafflestudio.webgam.domain.event.exception.NonAccessibleObjectEventException
import com.wafflestudio.webgam.domain.event.exception.ObjectEventNotFoundException
import com.wafflestudio.webgam.domain.event.model.ObjectEvent
import com.wafflestudio.webgam.domain.event.repository.ObjectEventRepository
import com.wafflestudio.webgam.domain.`object`.exception.NonAccessiblePageObjectException
import com.wafflestudio.webgam.domain.`object`.exception.PageObjectNotFoundException
import com.wafflestudio.webgam.domain.`object`.repository.PageObjectRepository
import com.wafflestudio.webgam.domain.page.exception.NonAccessibleProjectPageException
import com.wafflestudio.webgam.domain.page.exception.ProjectPageNotFoundException
import com.wafflestudio.webgam.domain.page.repository.ProjectPageRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ObjectEventService(
    private val projectPageRepository: ProjectPageRepository,
    private val pageObjectRepository: PageObjectRepository,
    private val objectEventRepository: ObjectEventRepository,
) {
    fun getEvent(myId: Long, eventId: Long): SimpleResponse {
        val event = objectEventRepository.findUndeletedObjectEventById(eventId) ?: throw ObjectEventNotFoundException(eventId)
        if (!event.isAccessibleTo(myId)) throw NonAccessibleObjectEventException(eventId)

        return SimpleResponse(event)
    }

    @Transactional
    fun createEvent(myId: Long, request: CreateRequest): SimpleResponse {
        val pageObject = pageObjectRepository
            .findUndeletedPageObjectById(request.objectId!!) ?: throw PageObjectNotFoundException(request.objectId)
        if (!pageObject.isAccessibleTo(myId)) throw NonAccessiblePageObjectException(request.objectId)

        pageObject.event ?.let { throw MultipleEventAllocationException(request.objectId) }

        val nextPage = request.nextPageId?.let {
            val page = projectPageRepository.findUndeletedProjectPageById(it) ?: throw ProjectPageNotFoundException(request.nextPageId)
            if (!page.isAccessibleTo(myId)) throw NonAccessibleProjectPageException(request.nextPageId)

            if (page.project != pageObject.page.project) throw LinkNonRelatedPageException(request.nextPageId)

            page
        }

        val event = objectEventRepository.save(ObjectEvent(request, pageObject, nextPage))

        return SimpleResponse(event)
    }

    @Transactional
    fun updateEvent(myId: Long, eventId: Long, request: PatchRequest): SimpleResponse {
        val event = objectEventRepository.findUndeletedObjectEventById(eventId) ?: throw ObjectEventNotFoundException(eventId)
        if (!event.isAccessibleTo(myId)) throw NonAccessibleObjectEventException(eventId)

        request.transitionType ?.let { event.transitionType = it }
        request.nextPageId?.let {
            val page = projectPageRepository.findUndeletedProjectPageById(it) ?: throw ProjectPageNotFoundException(request.nextPageId)
            if (!page.isAccessibleTo(myId)) throw NonAccessibleProjectPageException(request.nextPageId)

            if (page.project != event.`object`.page.project) throw LinkNonRelatedPageException(request.nextPageId)

            event.nextPage = page
            page.triggeredEvents.add(event)
        }

        return SimpleResponse(event)
    }

    @Transactional
    fun deleteEvent(myId: Long, eventId: Long) {
        val event = objectEventRepository.findUndeletedObjectEventById(eventId) ?: throw ObjectEventNotFoundException(eventId)
        if (!event.isAccessibleTo(myId)) throw NonAccessibleObjectEventException(eventId)

        event.delete()
    }
}