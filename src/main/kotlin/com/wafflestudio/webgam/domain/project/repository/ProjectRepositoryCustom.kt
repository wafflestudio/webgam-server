package com.wafflestudio.webgam.domain.project.repository

import com.wafflestudio.webgam.domain.project.model.Project

interface ProjectRepositoryCustom {
    fun findUndeletedProjectById(id: Long): Project?
    fun findAllByOwnerIdEquals(ownerId: Long): List<Project>
}