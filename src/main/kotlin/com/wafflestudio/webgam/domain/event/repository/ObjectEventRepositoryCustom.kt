package com.wafflestudio.webgam.domain.event.repository

import com.wafflestudio.webgam.domain.event.model.ObjectEvent

interface ObjectEventRepositoryCustom {
    fun findUndeletedObjectEventById(id: Long): ObjectEvent?
    fun findByObjectId(objectId: Long): ObjectEvent?
}