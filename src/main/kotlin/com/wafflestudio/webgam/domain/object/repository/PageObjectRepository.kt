package com.wafflestudio.webgam.domain.`object`.repository

import com.wafflestudio.webgam.domain.`object`.model.PageObject
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PageObjectRepository : JpaRepository<PageObject, Long>, PageObjectRepositoryCustom {
}