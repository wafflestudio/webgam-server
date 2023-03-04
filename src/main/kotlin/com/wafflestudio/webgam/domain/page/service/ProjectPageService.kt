package com.wafflestudio.webgam.domain.page.service

import com.wafflestudio.webgam.domain.page.dto.ProjectPageDto.*
import com.wafflestudio.webgam.domain.page.exception.NonAccessibleProjectPageException
import com.wafflestudio.webgam.domain.page.exception.ProjectPageNotFoundException
import com.wafflestudio.webgam.domain.page.model.ProjectPage
import com.wafflestudio.webgam.domain.page.repository.ProjectPageRepository
import com.wafflestudio.webgam.domain.project.exception.NonAccessibleProjectException
import com.wafflestudio.webgam.domain.project.exception.ProjectNotFoundException
import com.wafflestudio.webgam.domain.project.repository.ProjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProjectPageService (
        private val projectPageRepository: ProjectPageRepository,
        private val projectRepository: ProjectRepository
){
        fun getProjectPage(myId: Long, projectId: Long): DetailedResponse {
                val projectPage = projectPageRepository.findUndeletedProjectPageById(projectId)
                        ?: throw ProjectPageNotFoundException(projectId)
                if (!projectPage.isAccessibleTo(myId)) throw NonAccessibleProjectPageException(projectId)
                return DetailedResponse(projectPage)
        }

        @Transactional
        fun createProjectPage(myId: Long, request: CreateRequest): DetailedResponse {
                val project = projectRepository.findUndeletedProjectById(request.projectId)
                        ?: throw ProjectNotFoundException(request.projectId)
                if (!project.isAccessibleTo(myId)) throw NonAccessibleProjectException(request.projectId)
                val projectPage = projectPageRepository.save(ProjectPage(project, request))
                return DetailedResponse(projectPage)
        }

        @Transactional
        fun patchProjectPage(myId: Long, id:Long, request: PatchRequest)
        : DetailedResponse{
                val projectPage = projectPageRepository.findUndeletedProjectPageById(id)
                        ?: throw ProjectPageNotFoundException(id)
                if (!projectPage.isAccessibleTo(myId)) throw NonAccessibleProjectPageException(id)
                request.name ?.let{projectPage.name = it}
                return DetailedResponse(projectPage)
        }

        @Transactional
        fun deleteProjectPage(myId: Long, id:Long)
        {
                val projectPage = projectPageRepository.findUndeletedProjectPageById(id)
                        ?: throw ProjectPageNotFoundException(id)
                if (!projectPage.isAccessibleTo(myId)) throw NonAccessibleProjectPageException(id)
                projectPage.delete()
        }


}