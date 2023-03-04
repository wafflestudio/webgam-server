package com.wafflestudio.webgam.domain.project.repository

import com.wafflestudio.webgam.domain.project.model.Project
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

interface ProjectRepositoryCustom {
    fun findUndeletedProjectById(id: Long): Project?
    fun findUndeletedAllByOwnerIdEquals(ownerId: Long): List<Project>
    fun findUndeletedAll(pageable: Pageable): Slice<Project>
}