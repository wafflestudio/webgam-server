package com.wafflestudio.webgam.domain.project.service

import com.wafflestudio.webgam.domain.project.dto.ProjectDto.*
import com.wafflestudio.webgam.domain.project.exception.NonAccessibleProjectException
import com.wafflestudio.webgam.domain.project.exception.ProjectNotFoundException
import com.wafflestudio.webgam.domain.project.model.Project
import com.wafflestudio.webgam.domain.project.repository.ProjectRepository
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import com.wafflestudio.webgam.global.common.dto.ListResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional(readOnly = true)
class ProjectService (
        private val projectRepository: ProjectRepository,
        private val userRepository: UserRepository
){

    fun getProject(myId: Long, projectId: Long): DetailedResponse {
        val project = projectRepository.findUndeletedProjectById(projectId) ?: throw ProjectNotFoundException(projectId)
        if (!project.isAccessibleTo(myId)) throw NonAccessibleProjectException(projectId)
        return DetailedResponse(project)
    }

    fun getProjectList(page: Int, size:Int): Slice<SimpleResponse> {
        val pageRequest = PageRequest.of(page, size)
        return projectRepository.findUndeletedAll(pageRequest).map { SimpleResponse(it) }
    }

    fun getUserProject(userId: Long): ListResponse<SimpleResponse> {
        val projects = projectRepository.findUndeletedAllByOwnerIdEquals(userId)
        return ListResponse(projects.map{ SimpleResponse(it) })
    }

    @Transactional
    fun createProject(myId: Long, request: CreateRequest): DetailedResponse {
        val me = userRepository.findUserById(myId)!!
        var project = Project(me, request)
        project = projectRepository.save(project)
        return DetailedResponse(project)
    }

    @Transactional
    fun patchProject(myId: Long, projectId: Long, request: PatchRequest): DetailedResponse {
        val project = projectRepository.findUndeletedProjectById(projectId) ?: throw ProjectNotFoundException(projectId)
        if (!project.isAccessibleTo(myId)) throw NonAccessibleProjectException(projectId)
        request.title ?.let { project.title = it }
        return DetailedResponse(project)
    }

    @Transactional
    fun deleteProject(myId: Long, projectId: Long) {
        val project = projectRepository.findUndeletedProjectById(projectId) ?: throw ProjectNotFoundException(projectId)
        if (!project.isAccessibleTo(myId)) throw NonAccessibleProjectException(projectId)
        project.delete()
    }
}