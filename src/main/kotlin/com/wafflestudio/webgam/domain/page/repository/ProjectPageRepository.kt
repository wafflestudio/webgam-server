package com.wafflestudio.webgam.domain.page.repository;

import com.wafflestudio.webgam.domain.page.model.ProjectPage
import org.springframework.data.jpa.repository.JpaRepository

interface ProjectPageRepository : JpaRepository<ProjectPage, Long> {
    fun findByProjectIdAndName(projectId: Long, name: String): ProjectPage?
    fun findAllByProjectId(projectId: Long): List<ProjectPage>
}