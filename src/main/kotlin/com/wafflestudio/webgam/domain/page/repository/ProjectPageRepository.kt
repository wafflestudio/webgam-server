package com.wafflestudio.webgam.domain.page.repository

import com.wafflestudio.webgam.domain.page.model.ProjectPage
import org.springframework.data.jpa.repository.JpaRepository

interface ProjectPageRepository : JpaRepository<ProjectPage, Long>, ProjectPageRepositoryCustom {

}