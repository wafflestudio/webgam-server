package com.wafflestudio.webgam.domain.`object`.repository;

import com.wafflestudio.webgam.domain.`object`.model.PageObject
import org.springframework.data.jpa.repository.JpaRepository

interface PageObjectRepository : JpaRepository<PageObject, Long> {
}