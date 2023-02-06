package com.wafflestudio.webgam.domain.`object`.repository

import com.wafflestudio.webgam.domain.`object`.model.PageObject

interface PageObjectRepositoryCustom {
    fun findUndeletedPageObjectById(id: Long): PageObject?
    fun findAllUndeletedPageObjectsInProject(projectId: Long): List<PageObject>
}