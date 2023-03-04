package com.wafflestudio.webgam.domain.`object`.repository

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import com.wafflestudio.webgam.domain.event.model.QObjectEvent.objectEvent
import com.wafflestudio.webgam.domain.`object`.model.PageObject
import com.wafflestudio.webgam.domain.`object`.model.QPageObject.pageObject
import com.wafflestudio.webgam.domain.page.model.QProjectPage.projectPage
import com.wafflestudio.webgam.domain.project.model.QProject.project
import com.wafflestudio.webgam.domain.user.model.QUser.user
import org.springframework.stereotype.Repository

@Repository
class PageObjectRepositoryImpl(
    private val jpaQueryFactory: JPAQueryFactory
) : PageObjectRepositoryCustom {

    fun id(id: Long): BooleanExpression = pageObject.id.eq(id)
    fun undeletedPageObject(): BooleanExpression = pageObject.isDeleted.isFalse

    override fun findUndeletedPageObjectById(id: Long): PageObject? = jpaQueryFactory
        .select(pageObject)
        .from(pageObject)
        .leftJoin(pageObject.page, projectPage).fetchJoin()
        .leftJoin(pageObject.events, objectEvent).fetchJoin()
        .leftJoin(projectPage.project, project).fetchJoin()
        .leftJoin(project.owner, user).fetchJoin()
        .where(id(id), undeletedPageObject())
        .fetchOne()

    override fun findAllUndeletedPageObjectsInProject(projectId: Long): List<PageObject> = jpaQueryFactory
        .select(pageObject)
        .from(pageObject)
        .leftJoin(pageObject.page, projectPage).fetchJoin()
        .leftJoin(pageObject.events, objectEvent).fetchJoin()
        .leftJoin(projectPage.project, project).fetchJoin()
        .leftJoin(project.owner, user).fetchJoin()
        .where(project.id.eq(projectId), undeletedPageObject())
        .fetch()

}