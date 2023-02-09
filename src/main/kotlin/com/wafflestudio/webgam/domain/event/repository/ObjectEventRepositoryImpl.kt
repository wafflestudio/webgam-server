package com.wafflestudio.webgam.domain.event.repository

import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import com.wafflestudio.webgam.domain.event.model.ObjectEvent
import com.wafflestudio.webgam.domain.event.model.QObjectEvent.objectEvent
import com.wafflestudio.webgam.domain.`object`.model.QPageObject.pageObject
import com.wafflestudio.webgam.domain.page.model.QProjectPage.projectPage
import com.wafflestudio.webgam.domain.project.model.QProject.project
import com.wafflestudio.webgam.domain.user.model.QUser.user
import org.springframework.stereotype.Repository

@Repository
class ObjectEventRepositoryImpl(
    private val jpaQueryFactory: JPAQueryFactory,
): ObjectEventRepositoryCustom {

    fun id(id: Long): BooleanExpression = objectEvent.id.eq(id)
    fun undeletedObjectEvent(): BooleanExpression = objectEvent.isDeleted.isFalse

    private fun findByConditions(vararg conditions: Predicate): ObjectEvent? = jpaQueryFactory
        .select(objectEvent)
        .from(objectEvent)
        .leftJoin(objectEvent.`object`, pageObject).fetchJoin()
        .leftJoin(pageObject.page, projectPage).fetchJoin()
        .leftJoin(projectPage.project, project).fetchJoin()
        .leftJoin(project.owner, user).fetchJoin()
        .where(*conditions)
        .fetchOne()

    override fun findUndeletedObjectEventById(id: Long): ObjectEvent? = findByConditions(
        id(id), undeletedObjectEvent()
    )

    override fun findByObjectId(objectId: Long): ObjectEvent? = findByConditions(
        objectEvent.`object`.id.eq(objectId)
    )
}