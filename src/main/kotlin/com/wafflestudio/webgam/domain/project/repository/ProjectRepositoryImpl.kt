package com.wafflestudio.webgam.domain.project.repository

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import com.wafflestudio.webgam.domain.project.model.Project
import com.wafflestudio.webgam.domain.project.model.QProject.project
import com.wafflestudio.webgam.domain.user.model.QUser.user
import org.springframework.stereotype.Repository

@Repository
class ProjectRepositoryImpl(
    private val jpaQueryFactory: JPAQueryFactory
): ProjectRepositoryCustom {

    fun id(id: Long): BooleanExpression = project.id.eq(id)
    fun undeletedProject(): BooleanExpression = project.isDeleted.isFalse
    fun undeletedUser(): BooleanExpression = user.isDeleted.isFalse

    override fun findUndeletedProjectById(id: Long): Project? = jpaQueryFactory
        .select(project)
        .from(project)
        .leftJoin(project.owner, user).fetchJoin()
        .where(id(id), undeletedProject(), undeletedUser())
        .fetchOne()
}