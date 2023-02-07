package com.wafflestudio.webgam.domain.page.repository

import com.wafflestudio.webgam.domain.page.model.ProjectPage

interface ProjectPageRepositoryCustom {
    fun findUndeletedProjectPageById(id: Long): ProjectPage?
    fun findUndeletedProjectPageByProjectIdAndName(projectId: Long, name: String): ProjectPage?
    fun findAllUndeletedProjectPageByProjectId(projectId: Long): List<ProjectPage>
}