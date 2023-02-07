package com.wafflestudio.webgam.domain.project.repository

import com.wafflestudio.webgam.domain.project.model.Project
import org.springframework.data.jpa.repository.JpaRepository

interface ProjectRepository : JpaRepository<Project, Long> {
    fun findProjectById(id: Long): Project?
    fun findAllByOwnerIdEquals(ownerId: Long): List<Project>

}