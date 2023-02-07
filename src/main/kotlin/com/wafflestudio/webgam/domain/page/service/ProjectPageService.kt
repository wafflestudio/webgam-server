package com.wafflestudio.webgam.domain.page.service

import com.wafflestudio.webgam.domain.page.dto.ProjectPageDto.SimpleResponse
import com.wafflestudio.webgam.domain.page.dto.ProjectPageDto.DetailedResponse
import com.wafflestudio.webgam.domain.page.dto.ProjectPageDto.CreateRequest
import com.wafflestudio.webgam.domain.page.dto.ProjectPageDto.PatchRequest
import com.wafflestudio.webgam.domain.page.exception.NonAccessibleProjectPageException
import com.wafflestudio.webgam.domain.page.exception.ProjectPageNotFoundException
import com.wafflestudio.webgam.domain.page.model.ProjectPage
import com.wafflestudio.webgam.domain.page.repository.ProjectPageRepository
import com.wafflestudio.webgam.domain.project.exception.NonAccessibleProjectException
import com.wafflestudio.webgam.domain.project.exception.ProjectNotFoundException
import com.wafflestudio.webgam.domain.project.repository.ProjectRepository
import com.wafflestudio.webgam.domain.project.service.ProjectService
import com.wafflestudio.webgam.global.common.dto.ListResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProjectPageService (
        private val projectPageRepository: ProjectPageRepository,
        private val projectRepository: ProjectRepository
){
        fun getProjectPage(myId: Long, projectId: Long, name: String): DetailedResponse {
                val projectPage = projectPageRepository.findUndeletedProjectPageByProjectIdAndName(projectId, name)
                        ?: throw ProjectPageNotFoundException(projectId, name)
                if (!projectPage.isAccessibleTo(myId)) throw NonAccessibleProjectPageException(projectId, name)
                return DetailedResponse(projectPage)
        }

        fun getProjectPages(myId: Long, projectId: Long): ListResponse<SimpleResponse> {
                val project = projectRepository.findUndeletedProjectById(projectId)
                        ?: throw ProjectNotFoundException(projectId)
                if (!project.isAccessibleTo(myId)) throw NonAccessibleProjectException(projectId)
                val projectPages = projectPageRepository.findAllUndeletedProjectPageByProjectId(projectId)

                val accessiblePages = mutableListOf<ProjectPage>()
                for (projectPage in projectPages){
                        if (projectPage.isAccessibleTo(myId)) accessiblePages.add(projectPage)
                }
                return ListResponse(accessiblePages.map{ SimpleResponse(it)})
        }

        fun createProjectPage(myId: Long, projectId: Long, request: CreateRequest): DetailedResponse {
                val project = projectRepository.findUndeletedProjectById(projectId)
                        ?: throw ProjectNotFoundException(projectId)
                if (!project.isAccessibleTo(myId)) throw NonAccessibleProjectException(projectId)
                val projectPage = ProjectPage(project, request)
                return DetailedResponse(projectPage)
        }

        fun patchProjectPage(myId: Long, projectId: Long, name: String, request: PatchRequest)
        : DetailedResponse{
                val projectPage = projectPageRepository.findUndeletedProjectPageByProjectIdAndName(projectId, name)
                        ?: throw ProjectPageNotFoundException(projectId, name)
                if (!projectPage.isAccessibleTo(myId)) throw NonAccessibleProjectPageException(projectId, name)
                request.name ?.let{projectPage.name = it}
                return DetailedResponse(projectPage)
        }

        fun deleteProjectPage(myId: Long, projectId: Long, name:String)
        : SimpleResponse {
                val projectPage = projectPageRepository.findUndeletedProjectPageByProjectIdAndName(projectId, name)
                        ?: throw ProjectPageNotFoundException(projectId, name)
                if (!projectPage.isAccessibleTo(myId)) throw NonAccessibleProjectPageException(projectId, name)
                projectPage.isDeleted = true
                return SimpleResponse(projectPage)
        }


}