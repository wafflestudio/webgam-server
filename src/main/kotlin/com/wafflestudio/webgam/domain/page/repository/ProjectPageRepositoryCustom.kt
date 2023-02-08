package com.wafflestudio.webgam.domain.page.repository

import com.wafflestudio.webgam.domain.page.model.ProjectPage

interface ProjectPageRepositoryCustom {
    fun findUndeletedProjectPageById(id: Long): ProjectPage?
}