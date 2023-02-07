package com.wafflestudio.webgam.domain.project.service

import com.wafflestudio.webgam.domain.project.dto.ProjectDto.CreateRequest
import com.wafflestudio.webgam.domain.project.dto.ProjectDto.PatchRequest
import com.wafflestudio.webgam.domain.project.dto.ProjectDto.DetailedResponse
import com.wafflestudio.webgam.domain.project.dto.ProjectDto.SimpleResponse
import com.wafflestudio.webgam.domain.project.exception.NonAccessibleProjectException
import com.wafflestudio.webgam.global.common.dto.PageResponse
import com.wafflestudio.webgam.domain.project.exception.ProjectNotFoundException
import com.wafflestudio.webgam.domain.project.model.Project
import com.wafflestudio.webgam.domain.project.repository.ProjectRepository
import com.wafflestudio.webgam.domain.user.repository.UserRepository
import com.wafflestudio.webgam.domain.user.service.UserService
import com.wafflestudio.webgam.global.common.dto.ListResponse
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional(readOnly = true)
class ProjectService (
        private val projectRepository: ProjectRepository,
        private val userService: UserService,
        private val userRepository: UserRepository
){

    fun getProject(myId: Long, projectId: Long): DetailedResponse {
        val project = projectRepository.findProjectById(projectId) ?: throw ProjectNotFoundException(projectId)
        if (!project.isAccessibleTo(myId)) throw NonAccessibleProjectException(projectId)
        return DetailedResponse(project)
    }

    fun getProjectList(page: Int, size:Int): PageResponse<SimpleResponse> {
        val pageRequest = PageRequest.of(page, size)
        val projects = projectRepository.findAll(pageRequest)
        return PageResponse(projects.content.map{SimpleResponse(it)}, page, size, projects.size)
    }

    fun getUserProject(userId: Long): ListResponse<SimpleResponse> {
        val projects = projectRepository.findAllByOwnerIdEquals(userId)
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
        val project = projectRepository.findProjectById(projectId) ?: throw ProjectNotFoundException(projectId)
        if (!project.isAccessibleTo(myId)) throw NonAccessibleProjectException(projectId)
        request.title ?.let { project.title = it }
        return DetailedResponse(project)
    }

    @Transactional
    fun deleteProject(myId: Long, projectId: Long): DetailedResponse {
        val project = projectRepository.findProjectById(projectId) ?: throw ProjectNotFoundException(projectId)
        if (!project.isAccessibleTo(myId)) throw NonAccessibleProjectException(projectId)
        project.isDeleted = true
        return DetailedResponse(project)
    }
}