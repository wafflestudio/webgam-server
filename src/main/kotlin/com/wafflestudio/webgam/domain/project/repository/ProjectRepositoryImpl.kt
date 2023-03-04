package com.wafflestudio.webgam.domain.project.repository

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import com.wafflestudio.webgam.domain.project.model.Project
import com.wafflestudio.webgam.domain.project.model.QProject.project
import com.wafflestudio.webgam.domain.user.model.QUser.user
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl
import org.springframework.stereotype.Repository

@Repository
class ProjectRepositoryImpl(
    private val jpaQueryFactory: JPAQueryFactory
): ProjectRepositoryCustom {

    fun id(id: Long): BooleanExpression = project.id.eq(id)
    fun undeletedProject(): BooleanExpression = project.isDeleted.isFalse

    override fun findUndeletedProjectById(id: Long): Project? = jpaQueryFactory
        .select(project)
        .from(project)
        .leftJoin(project.owner, user).fetchJoin()
        .where(id(id), undeletedProject())
        .fetchOne()

    override fun findUndeletedAllByOwnerIdEquals(ownerId: Long): List<Project> = jpaQueryFactory
            .select(project)
            .from(project)
            .leftJoin(project.owner, user).fetchJoin()
            .where(user.id.eq(ownerId), undeletedProject())
            .fetch()

    override fun findUndeletedAll(pageable: Pageable): Slice<Project> {
        val projects = jpaQueryFactory
            .select(project)
            .from(project)
            .leftJoin(project.owner, user).fetchJoin()
            .where(undeletedProject())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        var hasNext = false
        if (projects.size > pageable.pageSize) {
            projects.removeAt(pageable.pageSize)
            hasNext = true
        }

        return SliceImpl(projects, pageable, hasNext)
    }
}