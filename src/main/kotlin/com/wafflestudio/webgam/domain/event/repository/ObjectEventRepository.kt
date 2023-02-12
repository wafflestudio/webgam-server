package com.wafflestudio.webgam.domain.event.repository

import com.wafflestudio.webgam.domain.event.model.ObjectEvent
import org.springframework.data.jpa.repository.JpaRepository

interface ObjectEventRepository : JpaRepository<ObjectEvent, Long>, ObjectEventRepositoryCustom {
}