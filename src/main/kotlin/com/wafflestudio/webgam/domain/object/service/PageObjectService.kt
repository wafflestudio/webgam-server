package com.wafflestudio.webgam.domain.`object`.service

import com.wafflestudio.webgam.domain.`object`.dto.PageObjectDto.*
import com.wafflestudio.webgam.domain.`object`.exception.NonAccessiblePageObjectException
import com.wafflestudio.webgam.domain.`object`.exception.PageObjectNotFoundException
import com.wafflestudio.webgam.domain.`object`.model.PageObject
import com.wafflestudio.webgam.domain.`object`.repository.PageObjectRepository
import com.wafflestudio.webgam.domain.page.exception.NonAccessibleProjectPageException
import com.wafflestudio.webgam.domain.page.exception.ProjectPageNotFoundException
import com.wafflestudio.webgam.domain.page.repository.ProjectPageRepository
import com.wafflestudio.webgam.domain.project.exception.NonAccessibleProjectException
import com.wafflestudio.webgam.domain.project.exception.ProjectNotFoundException
import com.wafflestudio.webgam.domain.project.repository.ProjectRepository
import com.wafflestudio.webgam.global.common.dto.ListResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PageObjectService(
    private val projectRepository: ProjectRepository,
    private val projectPageRepository: ProjectPageRepository,
    private val pageObjectRepository: PageObjectRepository,
) {
    fun listProjectObjects(myId: Long, projectId: Long): ListResponse<DetailedResponse> {
        val project = projectRepository.findUndeletedProjectById(projectId) ?: throw ProjectNotFoundException(projectId)
        if (!project.isAccessibleTo(myId)) throw NonAccessibleProjectException(projectId)

        val objects = pageObjectRepository.findAllUndeletedPageObjectsInProject(projectId)
            .filter { it.isAccessibleTo(myId) }
            .map { DetailedResponse(it) }
            .toList()

        return ListResponse(objects)
    }

    fun getObject(myId: Long, objectId: Long): DetailedResponse {
        val pageObject = pageObjectRepository.findUndeletedPageObjectById(objectId) ?: throw PageObjectNotFoundException(objectId)
        if (!pageObject.isAccessibleTo(myId)) throw NonAccessiblePageObjectException(objectId)

        return DetailedResponse(pageObject)
    }

    @Transactional
    fun createObject(myId: Long, request: CreateRequest): SimpleResponse {
        val page = projectPageRepository.findUndeletedProjectPageById(request.pageId!!) ?: throw ProjectPageNotFoundException(request.pageId)
        if (!page.isAccessibleTo(myId)) throw NonAccessibleProjectPageException(request.pageId)

        val pageObject = pageObjectRepository.save(PageObject(page, request))

        return SimpleResponse(pageObject)
    }

    @Transactional
    fun modifyObject(myId: Long, objectId: Long, request: PatchRequest): DetailedResponse {
        val pageObject = pageObjectRepository.findUndeletedPageObjectById(objectId) ?: throw PageObjectNotFoundException(objectId)
        if (!pageObject.isAccessibleTo(myId)) throw NonAccessiblePageObjectException(objectId)

        request.type ?.let { pageObject.type = request.type }
        request.width ?.let { pageObject.width = request.width }
        request.height ?.let { pageObject.height = request.height }
        request.xPosition ?.let { pageObject.xPosition = request.xPosition }
        request.yPosition ?.let { pageObject.yPosition = request.yPosition }
        request.zIndex ?.let { pageObject.zIndex = request.zIndex }
        request.textContent ?.let { pageObject.textContent = request.textContent }
        request.fontSize ?.let { pageObject.fontSize = request.fontSize }
        request.imageSource ?.let { pageObject.imageSource = request.imageSource }

        return DetailedResponse(pageObject)
    }

    @Transactional
    fun deleteObject(myId: Long, objectId: Long) {
        val pageObject = pageObjectRepository.findUndeletedPageObjectById(objectId) ?: throw PageObjectNotFoundException(objectId)
        if (!pageObject.isAccessibleTo(myId)) throw NonAccessiblePageObjectException(objectId)

        pageObject.delete()
    }
}