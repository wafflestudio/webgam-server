package com.wafflestudio.webgam.domain.page.repository

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import com.wafflestudio.webgam.domain.page.model.ProjectPage
import com.wafflestudio.webgam.domain.page.model.QProjectPage.projectPage
import com.wafflestudio.webgam.domain.project.model.QProject.project
import com.wafflestudio.webgam.domain.user.model.QUser.user
import org.springframework.stereotype.Repository

@Repository
class ProjectPageRepositoryImpl(
    private val jpaQueryFactory: JPAQueryFactory
): ProjectPageRepositoryCustom {

    fun id(id: Long): BooleanExpression = projectPage.id.eq(id)
    fun undeletedProjectPage(): BooleanExpression = projectPage.isDeleted.isFalse
    fun undeletedProject(): BooleanExpression = project.isDeleted.isFalse
    fun undeletedUser(): BooleanExpression = user.isDeleted.isFalse

    override fun findUndeletedProjectPageById(id: Long): ProjectPage? = jpaQueryFactory
        .select(projectPage)
        .from(projectPage)
        .leftJoin(projectPage.project, project).fetchJoin()
        .leftJoin(project.owner, user).fetchJoin()
        .where(id(id), undeletedProjectPage(), undeletedProject(), undeletedUser())
        .fetchOne()

}